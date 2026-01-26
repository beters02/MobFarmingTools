package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobFanInitializer extends RefSystem<ChunkStore> {

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        BlockModule.BlockStateInfo info = commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;

        MobFanComponent mobFan = commandBuffer.getComponent(ref, MobFarmingToolsPlugin.get().getMobFanComponentType());

        if (mobFan != null) {
            int localX = ChunkUtil.xFromBlockInColumn(info.getIndex());
            int worldY = ChunkUtil.yFromBlockInColumn(info.getIndex());
            int localZ = ChunkUtil.zFromBlockInColumn(info.getIndex());

            Store<ChunkStore> chunkStore = info.getChunkRef().getStore();
            World world = chunkStore.getExternalData().getWorld();
            WorldChunk worldChunk = chunkStore.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

            if (worldChunk == null) {
                MobFarmingToolsPlugin.LOGGER.atWarning().log("WORLD CHUNK NOT FOUND!");
                return;
            }

            int chunkX = worldChunk.getX();
            int chunkZ = worldChunk.getZ();

            int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
            int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

            int rotationIndex = world.getBlockRotationIndex(worldX, worldY, worldZ);

            mobFan.setStoredWorld(world);
            mobFan.setStoredWorldPos(new Vector3i(worldX, worldY, worldZ));
            MobFarmingToolsPlugin.LOGGER.atInfo().log("MobFanComponent successfully initialized.");

            mobFan.setEnabled(true);

            MobFarmingToolsPlugin.LOGGER.atInfo().log("Rotation index of added mob fan: " + rotationIndex);
        }
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
            BlockModule.BlockStateInfo.getComponentType(),
            MobFarmingToolsPlugin.get().getMobFanComponentType()
        );
    }
}
