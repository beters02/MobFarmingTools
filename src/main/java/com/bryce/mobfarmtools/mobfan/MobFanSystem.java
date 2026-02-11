package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTChunkUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobFanSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobFanSystem]");

    public MobFanSystem(ComponentType<ChunkStore, MobFanComponent> mobFanComponentType) {
        this.mobFanComponentType = mobFanComponentType;
        debugger.setEnabled(false);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.mobFanComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

        MobFanComponent fan = archetypeChunk.getComponent(index, this.mobFanComponentType);
        if (fan == null) {
            debugger.atWarning("MOB FAN COMPONENT NOT FOUND!");
            return;
        }

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) {
            debugger.atWarning("BLOCK STATE INFO NOT FOUND!");
            return;
        }

        Vector3d worldPos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (worldPos == null) {
            debugger.atWarning("WORLD POS NOT FOUND!");
            return;
        }
        Vector3i worldPosI = worldPos.toVector3i();

        if (MFTChunkUtil.IsChunkLoaded(info.getChunkRef().getStore().getExternalData().getWorld(), worldPosI.x, worldPosI.z) == null) {
            debugger.atWarning("Chunk is not loaded");
            return;
        }

        Store<ChunkStore> chunkStore = info.getChunkRef().getStore();
        World world = chunkStore.getExternalData().getWorld();
        int rotationIndex = world.getBlockRotationIndex(worldPosI.x, worldPosI.y, worldPosI.z);

        fan.tickAction(dt, worldPosI.x, worldPosI.y, worldPosI.z, rotationIndex, world);
    }
}
