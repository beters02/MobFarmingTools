package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MFTBlockUtil {

    public static @Nullable Vector3d GetWorldPosFromBlockStateInfo(BlockModule.BlockStateInfo info) {
        int blockIndex = info.getIndex();

        int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
        int worldY = ChunkUtil.yFromBlockInColumn(blockIndex);
        int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);

        Store<ChunkStore> chunkStore = info.getChunkRef().getStore();
        WorldChunk worldChunk = chunkStore.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

        if (worldChunk == null) {
            return null;
        }

        int chunkX = worldChunk.getX();
        int chunkZ = worldChunk.getZ();

        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

        return new Vector3d(worldX, worldY, worldZ);
    }

    public static BlockModule.BlockStateInfo GetBlockStateInfoFromArchetype(
            ArchetypeChunk<ChunkStore> archetypeChunk,
            int blockIndex
    ) {
        return archetypeChunk.getComponent(blockIndex, BlockModule.BlockStateInfo.getComponentType());
    }

    public static boolean PositionIsBlock(World world, Vector3i pos) {
        BlockType blockType = world.getBlockType(pos);
        return blockType == null
                || blockType == BlockType.EMPTY
                || blockType.getMaterial() == BlockMaterial.Empty;
    }


}
