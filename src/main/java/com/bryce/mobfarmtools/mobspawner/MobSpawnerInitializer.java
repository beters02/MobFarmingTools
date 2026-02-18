package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.chunks.ForcedChunkPersistence;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.ui.MobFanUpgradePage;
import com.bryce.mobfarmtools.mobspawner.ui.MobSpawnerUpgradePage;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterComponent;
import com.bryce.mobfarmtools.sounds.SoundManager;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MobSpawnerInitializer extends RefSystem<ChunkStore> {
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobSpawnerInitializer]", MobSpawnerConstants.DEBUGGER_ENABLED);

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobSpawnerComponent spawnerComponent = store.getComponent(ref, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) {
            debugger.atWarning("Could not find spawner component on spawner added.");
            return;
        }

        MFTSoundEmitterComponent emitter = store.getComponent(ref, MFTSoundEmitterComponent.getComponentType());
        if (emitter == null) {
            // fix previously placed vacuum hoppers not having its sound emitter component
            String[] soundIds = {"SFX_MFT_Spawner_Woosh_Quick"};
            commandBuffer.putComponent(ref, MFTSoundEmitterComponent.getComponentType(), new MFTSoundEmitterComponent(soundIds));
        }

        debugger.atInfo("CURRENT SPAWN RATE: " + spawnerComponent.getCurrentSpawnRate());
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        SoundManager.DestroyAllForBlock(ref);

        World world = store.getExternalData().getWorld();
        MobSpawnerUpgradePage.clearAllPreviews(world);

        if (removeReason == RemoveReason.UNLOAD) return;

        Vector3i pos = MFTBlockUtil.GetWorldPosFromBlockRef(store, ref);
        if (pos == null) return;

        MobSpawnerComponent mobSpawner = store.getComponent(ref, MobSpawnerComponent.getComponentType());
        if (mobSpawner != null && mobSpawner.isChunkLoaded()) {
            ForcedChunkPersistence.setForced(world, pos, false);
        }

        MachineUpgradeComponent upgrades = store.getComponent(ref, MachineUpgradeComponent.getComponentType());
        if (upgrades == null) return;

        List<ItemStack> drops = upgrades.getUpgradeDrops();
        if (drops.isEmpty()) return;

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Vector3d dropPosition = pos.toVector3d().add(0.5, 0.0, 0.5);
        Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(entityStore, drops, dropPosition, Vector3f.ZERO);
        if (holders.length > 0) {
            world.execute(() -> entityStore.addEntities(holders, AddReason.SPAWN));
        }
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                MobSpawnerComponent.getComponentType()
        );
    }
}
