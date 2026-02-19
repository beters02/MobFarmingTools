package com.bryce.mobfarmtools.mobmasher;

import com.bryce.mobfarmtools.chunks.ForcedChunkPersistence;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.ui.MobFanUpgradePage;
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
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MobMasherInitializer extends RefSystem<ChunkStore> {
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobMasherInitializer]");

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                MobMasherComponent.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

        MFTSoundEmitterComponent emitter = store.getComponent(ref, MFTSoundEmitterComponent.getComponentType());
        if (emitter == null) {
            // fix previously placed vacuum hoppers not having its sound emitter component
            String[] soundIds = {"SFX_MFT_Mob_Masher_Clang"};
            emitter = new MFTSoundEmitterComponent(soundIds);
            commandBuffer.putComponent(ref, MFTSoundEmitterComponent.getComponentType(), emitter);
        }

        SoundManager.StopAllForBlock(ref);

        // set enabled so on state animation is played

        MobMasherComponent masher = store.getComponent(ref, MobMasherComponent.getComponentType());
        if (masher == null) {
            debugger.atWarning("Failed to set enabled during init; MasherComponent is null.");
            return;
        }

        Vector3i blockPos = MFTBlockUtil.GetWorldPosFromBlockRef(store, ref);
        if (blockPos == null) {
            debugger.atWarning("Failed to set enabled during init; BlockPosition is null");
            return;
        }

        masher.setEnabled(ref, blockPos, masher.isEnabled());
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        SoundManager.DestroyAllForBlock(ref);

        World world = store.getExternalData().getWorld();
        MobFanUpgradePage.clearAllPreviews(world);

        if (removeReason == RemoveReason.UNLOAD) return;

        Vector3i pos = MFTBlockUtil.GetWorldPosFromBlockRef(store, ref);
        if (pos == null) return;

        MobFanComponent mobFan = commandBuffer.getComponent(ref, MobFanComponent.getComponentType());
        if (mobFan != null && mobFan.isChunkLoaded()) {
            ForcedChunkPersistence.setForced(world, pos, false);
        }

        MachineUpgradeComponent upgrades = store.getComponent(ref, MachineUpgradeComponent.getComponentType());
        if (upgrades == null) {
            return;
        }

        List<ItemStack> drops = upgrades.getUpgradeDrops();
        if (drops.isEmpty()) return;

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Vector3d dropPosition = pos.toVector3d().add(0.5, 0.0, 0.5);
        Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(entityStore, drops, dropPosition, Vector3f.ZERO);
        if (holders.length > 0) {
            world.execute(() -> entityStore.addEntities(holders, AddReason.SPAWN));
        }
    }
}
