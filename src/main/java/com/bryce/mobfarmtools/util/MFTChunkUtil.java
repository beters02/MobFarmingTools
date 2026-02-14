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

}
