package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import org.jspecify.annotations.Nullable;

public class MFTChunkUtil {

    public static @Nullable WorldChunk IsChunkLoaded(World world, long chunkIndex) {
        return world.getChunkIfLoaded(chunkIndex);
    }

    public static @Nullable WorldChunk IsChunkLoaded(World world, int blockX, int blockZ) {
        return IsChunkLoaded(world, ChunkUtil.indexChunkFromBlock(blockX, blockZ));
    }

    public static void EnableChunkLoadIfLoaded(World world, BlockPosition blockPosition) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null) return;
        worldChunk.addKeepLoaded();
    }

    public static void DisableChunkLoadIfLoaded(World world, BlockPosition blockPosition) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk == null) return;
        worldChunk.removeKeepLoaded();
    }

    public static void EnableChunkLoadUnsafe(World world, BlockPosition blockPosition) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        if (worldChunk == null) return;
        worldChunk.addKeepLoaded();
    }

    public static void DisableChunkLoadUnsafe(World world, BlockPosition blockPosition) {
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunk(chunkIndex);
        if (worldChunk == null) return;
        worldChunk.removeKeepLoaded();
    }

}
