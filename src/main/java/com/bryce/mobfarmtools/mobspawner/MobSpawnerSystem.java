package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobSpawnerSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType;

    public MobSpawnerSystem(ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType) {
        this.mobSpawnerComponentType = mobSpawnerComponentType;
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobSpawnerComponent spawnerComponent = archetypeChunk.getComponent(index, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) {
            return;
        }

        if (!spawnerComponent.canTick()) {
            return;
        }

        spawnerComponent.incrementLifetime(dt);

        if(spawnerComponent.canSpawn()) {

            if (spawnerComponent.getFailedTries() >= MobSpawnerConstants.MAX_FAILED_SPAWN_TRIES) {
                MobFarmingToolsPlugin.LOGGER.atWarning().log(
                        MobSpawnerConstants.MAX_FAILED_SPAWN_TRIES
                        + " failed spawn attempts on spawner <id>."
                );
                resetSpawnerVars(spawnerComponent);
                return;
            }

            Store<EntityStore> entityStore = store.getExternalData().getWorld().getEntityStore().getStore();

            BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
            if (info == null) {
                spawnerComponent.incrementFailedTries(1);
                return;
            }

            Vector3d worldPos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
            if (worldPos == null) {
                spawnerComponent.incrementFailedTries(1);
                return;
            }

            // TODO: RAY CAST AROUND SPAWNER TO FIND SPAWN LOCATION
            worldPos.y += 1;

            String entityId = spawnerComponent.getEntityId();

            resetSpawnerVars(spawnerComponent);
            NPCPlugin.get().spawnNPC(entityStore, entityId, null, worldPos, new Vector3f());
        }
    }

    private void resetSpawnerVars(MobSpawnerComponent spawnerComponent) {
        spawnerComponent.setLifetime(0);
        spawnerComponent.setRandomCurrentSpawnRate();
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.mobSpawnerComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }
}
