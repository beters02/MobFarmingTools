package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobFanSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;

    public MobFanSystem(ComponentType<ChunkStore, MobFanComponent> mobFanComponentType) {
        this.mobFanComponentType = mobFanComponentType;
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
            MobFarmingToolsPlugin.LOGGER.atWarning().log("MOB FAN COMPONENT NOT FOUND!");
            return;
        }

        BlockModule.BlockStateInfo info = archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log("BLOCK STATE INFO NOT FOUND!");
            return;
        }

        int blockIndex = info.getIndex();

        int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
        int worldY = ChunkUtil.yFromBlockInColumn(blockIndex);
        int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);

        //int localY = worldY & 31;

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

        fan.tickAction(dt, worldX, worldY, worldZ, rotationIndex, world);
    }
}
