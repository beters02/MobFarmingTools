package com.bryce.mobfarmtools.chunks;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import java.util.Set;

public final class ForcedChunkPersistence {
    private ForcedChunkPersistence() {}
    private static MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[ForcedChunkPersistence]", true);

    public static void setForced(World world, int x, int z, boolean forced) {
        Runnable write = () -> {
            Store<ChunkStore> store = world.getChunkStore().getStore();
            long idx = ChunkUtil.indexChunkFromBlock(x, z);

            ResourceType<ChunkStore, ForcedChunkRefCountResource> type =
                    MobFarmingToolsPlugin.get().getForcedChunkRefCountResourceType();
            ForcedChunkRefCountResource resource = store.getResource(type);

            if (forced) {
                resource.increment(idx);
                debugger.atInfo("SET CHUNK TO BE LOADED!");
            } else {
                resource.decrement(idx);
                debugger.atInfo("REMOVED CHUNK FROM LOADED!");
                if (resource.getCount(idx) == 0) {
                    WorldChunk worldChunk = world.getChunk(idx);
                    if (worldChunk == null) {
                        MobFarmingToolsPlugin.LOGGER.atWarning().log(
                                "Failed attempt to unload WorldChunk " + idx + " WORLD CHUNK IS NULL!"
                        );
                        return;
                    }
                    worldChunk.removeKeepLoaded();
                }
            }

            store.replaceResource(type, resource);
        };

        if (world.isInThread()) write.run();
        else world.execute(write);
    }

    public static void setForced(World world, BlockPosition pos, boolean forced) {
        setForced(world, pos.x, pos.z, forced);
    }

    public static void setForced(World world, Vector3i pos, boolean forced) {
        setForced(world, pos.x, pos.z, forced);
    }
}
