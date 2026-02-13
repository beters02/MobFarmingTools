package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
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

public class VacuumHopperInitializer extends RefSystem<ChunkStore> {
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[VacuumHopperInitializer]");

    public VacuumHopperInitializer() {
        debugger.setEnabled(false);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                VacuumHopperComponent.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        VacuumHopperComponent vacuumHopperComponent = store.getComponent(ref, VacuumHopperComponent.getComponentType());
        if (vacuumHopperComponent != null) {
            debugger.atWarning("Vacuum Hopper component found on init.");
        }
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        if (removeReason == RemoveReason.UNLOAD) {
            return;
        }

        MachineUpgradeComponent upgrades = store.getComponent(ref, MachineUpgradeComponent.getComponentType());
        if (upgrades == null) {
            return;
        }

        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null || info.getChunkRef() == null) {
            return;
        }

        Store<ChunkStore> chunkStore = info.getChunkRef().getStore();
        WorldChunk worldChunk = chunkStore.getComponent(info.getChunkRef(), WorldChunk.getComponentType());
        if (worldChunk == null) {
            return;
        }

        int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
        int worldY = ChunkUtil.yFromBlockInColumn(info.getIndex());
        int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());
        int worldX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), localZ);
        Vector3i pos = new Vector3i(worldX, worldY, worldZ);

        List<ItemStack> drops = new ArrayList<>();
        for (MachineUpgradeType type : MachineUpgradeType.values()) {
            int count = upgrades.getCount(type);
            if (count <= 0) {
                continue;
            }
            drops.add(new ItemStack(type.getItemId(), count));
        }
        if (drops.isEmpty()) {
            return;
        }

        World world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Vector3d dropPosition = pos.toVector3d().add(0.5, 0.0, 0.5);
        Holder<EntityStore>[] holders = ItemComponent.generateItemDrops(entityStore, drops, dropPosition, Vector3f.ZERO);
        if (holders.length > 0) {
            world.execute(() -> entityStore.addEntities(holders, AddReason.SPAWN));
        }
    }
}
