package com.bryce.mobfarmtools.chunks;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.dependency.Dependency;
import com.hypixel.hytale.component.dependency.Order;
import com.hypixel.hytale.component.dependency.SystemDependency;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkFlag;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.GetChunkFlags;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkUnloadingSystem;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class ChunkForceTickSystem extends TickingSystem<ChunkStore> {
    private static final int RETICK_INTERVAL_TICKS = 10;

    // runtime-only guard to avoid repeated addKeepLoaded increments
    private final Map<String, Set<Long>> appliedKeepLoadedByWorld = new HashMap<>();
    private final Map<String, Integer> tickCounterByWorld = new HashMap<>();

    @Nonnull
    @Override
    public Set<Dependency<ChunkStore>> getDependencies() {
        return Set.of(new SystemDependency<>(Order.BEFORE, ChunkUnloadingSystem.class));
    }

    @Override
    public void tick(float dt, int systemIndex, @Nonnull Store<ChunkStore> store) {
        World world = store.getExternalData().getWorld();
        String worldKey = world.getName();

        int tickCounter = tickCounterByWorld.getOrDefault(worldKey, 0) + 1;
        if (tickCounter < RETICK_INTERVAL_TICKS) {
            tickCounterByWorld.put(worldKey, tickCounter);
            return;
        }
        tickCounterByWorld.put(worldKey, 0);

        ResourceType<ChunkStore, ForcedChunkRefCountResource> resourceType =
                MobFarmingToolsPlugin.get().getForcedChunkRefCountResourceType();
        ForcedChunkRefCountResource resource = store.getResource(resourceType);

        Set<Long> desired = getDesiredChunkIndexes(resource);
        Set<Long> applied = appliedKeepLoadedByWorld.computeIfAbsent(worldKey, k -> new HashSet<>());

        // release chunks no longer desired
        Iterator<Long> it = applied.iterator();
        while (it.hasNext()) {
            long idx = it.next();
            if (!desired.contains(idx)) {
                WorldChunk chunk = world.getChunkIfInMemory(idx);
                if (chunk != null) {
                    chunk.removeKeepLoaded();
                }
                it.remove();
            }
        }

        // enforce desired chunks
        for (long idx : desired) {
            WorldChunk chunk = world.getChunkIfInMemory(idx);

            if (chunk == null) {
                // bootstrap load + ticking (important after server restart for far-away chunks)
                world.getChunkStore().getChunkReferenceAsync(idx, GetChunkFlags.SET_TICKING);
                continue;
            }

            if (applied.add(idx)) {
                chunk.addKeepLoaded();
            }

            // re-promote to ticking in case unload system turned it off
            chunk.resetActiveTimer();

            if (chunk.not(ChunkFlag.TICKING)) {
                world.loadChunkIfInMemory(idx);
            }
        }
    }

    private static Set<Long> getDesiredChunkIndexes(ForcedChunkRefCountResource resource) {
        long[] indexes = resource.getChunkIndexes();
        int[] counts = resource.getChunkCounts();
        int len = Math.min(indexes.length, counts.length);

        Set<Long> desired = new HashSet<>();
        for (int i = 0; i < len; i++) {
            if (counts[i] > 0) {
                desired.add(indexes[i]);
            }
        }
        return desired;
    }

    // --- Helpers for your upgrade/remove code paths ---

    public static void incrementForBlock(@Nonnull World world, @Nonnull BlockPosition pos) {
        increment(world, ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    }

    public static void decrementForBlock(@Nonnull World world, @Nonnull BlockPosition pos) {
        decrement(world, ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
    }

    public static void increment(@Nonnull World world, long chunkIndex) {
        mutateRefCount(world, chunkIndex, +1);
    }

    public static void decrement(@Nonnull World world, long chunkIndex) {
        mutateRefCount(world, chunkIndex, -1);
    }

    private static void mutateRefCount(@Nonnull World world, long chunkIndex, int delta) {
        Runnable op = () -> {
            Store<ChunkStore> store = world.getChunkStore().getStore();
            ResourceType<ChunkStore, ForcedChunkRefCountResource> type =
                    MobFarmingToolsPlugin.get().getForcedChunkRefCountResourceType();

            ForcedChunkRefCountResource resource = store.getResource(type);
            if (delta > 0) {
                resource.increment(chunkIndex);
            } else if (delta < 0) {
                resource.decrement(chunkIndex);
            }

            // explicit replace keeps behavior consistent with immutable-resource patterns
            store.replaceResource(type, resource);
        };

        if (world.isInThread()) {
            op.run();
        } else {
            world.execute(op);
        }
    }
}
