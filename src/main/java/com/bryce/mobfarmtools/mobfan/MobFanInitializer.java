package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.accessor.BlockAccessor;
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
            BlockType placedBlockType = world.getBlockType(worldX, worldY, worldZ);
            String placedBlockId = placedBlockType == null ? "" : placedBlockType.getId();
            boolean isFloorOrCeiling = placedBlockId.contains("Floor") || placedBlockId.contains("Ceiling");
            if (addReason == AddReason.SPAWN && isFloorOrCeiling) {
                int sectionY = ChunkUtil.indexSection(worldY);
                int localY = worldY & 31;
                rotationIndex = applyFloorCeilingRotation(
                    world,
                    worldChunk,
                    worldX,
                    worldY,
                    worldZ,
                    localX,
                    localY,
                    localZ,
                    sectionY,
                    rotationIndex,
                    chunkStore
                );
            }

            mobFan.setStoredWorld(world);
            mobFan.setStoredWorldPos(new Vector3i(worldX, worldY, worldZ));
            mobFan.setBaseForward(new Vector3d(0, 0, -1));
            
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

    private static int applyFloorCeilingRotation(
        @NonNull World world,
        @NonNull WorldChunk worldChunk,
        int worldX,
        int worldY,
        int worldZ,
        int localX,
        int localY,
        int localZ,
        int sectionY,
        int currentRotation,
        @NonNull Store<ChunkStore> chunkStore
    ) {
        BlockType below = world.getBlockType(worldX, worldY - 1, worldZ);
        BlockType above = world.getBlockType(worldX, worldY + 1, worldZ);
        boolean solidBelow = isSolid(below);
        boolean solidAbove = isSolid(above);

        if (solidBelow == solidAbove) {
            return currentRotation;
        }

        Rotation yaw = RotationTuple.get(currentRotation).yaw();
        Rotation pitch = solidBelow ? Rotation.Ninety : Rotation.TwoSeventy;
        int newRotation = RotationTuple.index(yaw, pitch, Rotation.None);

        if (newRotation == currentRotation) {
            return currentRotation;
        }

        int blockId = worldChunk.getBlock(localX, worldY, localZ);
        BlockType blockType = BlockType.getAssetMap().getAsset(blockId);
        if (blockType == null) {
            return currentRotation;
        }

        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReference(worldChunk.getX(), sectionY, worldChunk.getZ());
        if (sectionRef == null) {
            return currentRotation;
        }

        BlockSection blockSection = chunkStore.getComponent(sectionRef, BlockSection.getComponentType());
        if (blockSection == null) {
            return currentRotation;
        }

        int filler = blockSection.getFiller(localX, localY, localZ);
        worldChunk.setBlock(localX, worldY, localZ, blockId, blockType, newRotation, filler, 198);
        return newRotation;
    }

    private static boolean isSolid(@Nullable BlockType blockType) {
        return blockType != null && blockType != BlockType.EMPTY && blockType.getMaterial() != BlockMaterial.Empty;
    }
}
