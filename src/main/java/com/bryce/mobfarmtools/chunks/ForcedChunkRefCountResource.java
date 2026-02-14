package com.bryce.mobfarmtools.chunks;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Resource;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ForcedChunkRefCountResource implements Resource<ChunkStore> {
    public static final BuilderCodec<ForcedChunkRefCountResource> CODEC =
            BuilderCodec.builder(ForcedChunkRefCountResource.class, ForcedChunkRefCountResource::new)
                    .append(new KeyedCodec<>("ChunkIndexes", Codec.LONG_ARRAY),
                            ForcedChunkRefCountResource::setChunkIndexes,
                            ForcedChunkRefCountResource::getChunkIndexes)
                    .add()
                    .append(new KeyedCodec<>("ChunkCounts", Codec.INT_ARRAY),
                            ForcedChunkRefCountResource::setChunkCounts,
                            ForcedChunkRefCountResource::getChunkCounts)
                    .add()
                    .build();

    private long[] chunkIndexes = new long[0];
    private int[] chunkCounts = new int[0];

    public long[] getChunkIndexes() { return Arrays.copyOf(chunkIndexes, chunkIndexes.length); }
    public int[] getChunkCounts() { return Arrays.copyOf(chunkCounts, chunkCounts.length); }

    public void setChunkIndexes(long[] value) { chunkIndexes = value == null ? new long[0] : Arrays.copyOf(value, value.length); }
    public void setChunkCounts(int[] value) { chunkCounts = value == null ? new int[0] : Arrays.copyOf(value, value.length); }

    public Map<Long, Integer> toMap() {
        Map<Long, Integer> out = new HashMap<>();
        int len = Math.min(chunkIndexes.length, chunkCounts.length);
        for (int i = 0; i < len; i++) {
            int c = chunkCounts[i];
            if (c > 0) out.put(chunkIndexes[i], c);
        }
        return out;
    }

    public void fromMap(Map<Long, Integer> map) {
        long[] idx = new long[map.size()];
        int[] cnt = new int[map.size()];
        int i = 0;
        for (Map.Entry<Long, Integer> e : map.entrySet()) {
            int c = e.getValue() == null ? 0 : e.getValue();
            if (c <= 0) continue;
            idx[i] = e.getKey();
            cnt[i] = c;
            i++;
        }
        chunkIndexes = i == idx.length ? idx : Arrays.copyOf(idx, i);
        chunkCounts = i == cnt.length ? cnt : Arrays.copyOf(cnt, i);
    }

    public int increment(long chunkIndex) {
        Map<Long, Integer> map = toMap();
        int next = map.getOrDefault(chunkIndex, 0) + 1;
        map.put(chunkIndex, next);
        fromMap(map);
        return next;
    }

    public int decrement(long chunkIndex) {
        Map<Long, Integer> map = toMap();
        int curr = map.getOrDefault(chunkIndex, 0);
        int next = Math.max(0, curr - 1);
        if (next == 0) map.remove(chunkIndex);
        else map.put(chunkIndex, next);
        fromMap(map);
        return next;
    }

    public int getCount(long chunkIndex) {
        return toMap().getOrDefault(chunkIndex, 0);
    }

    public static ResourceType<ChunkStore, ForcedChunkRefCountResource> getResourceType() {
        return MobFarmingToolsPlugin.get().getForcedChunkRefCountResourceType();
    }

    @Nonnull
    @Override
    public Resource<ChunkStore> clone() {
        ForcedChunkRefCountResource copy = new ForcedChunkRefCountResource();
        copy.chunkIndexes = Arrays.copyOf(this.chunkIndexes, this.chunkIndexes.length);
        copy.chunkCounts = Arrays.copyOf(this.chunkCounts, this.chunkCounts.length);
        return copy;
    }
}
