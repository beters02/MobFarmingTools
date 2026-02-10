package com.bryce.mobfarmtools.spikes;

import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class SpikesSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, SpikesComponent> spikesComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SpikesSystem]");

    public SpikesSystem(ComponentType<ChunkStore, SpikesComponent> spikesComponentType) {
        this.spikesComponentType = spikesComponentType;
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.spikesComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float v, int i, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

    }
}
