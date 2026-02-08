package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTEntityUtil;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MobSpawnerSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType;

    public MobSpawnerSystem(ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType) {
        this.mobSpawnerComponentType = mobSpawnerComponentType;
    }

    private record SpawnResult(boolean ok, String msg) { }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobSpawnerComponent spawnerComponent = archetypeChunk.getComponent(index, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) { return; }
        if (!spawnerComponent.canTick()) { return; }

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

            SpawnResult spawnResult = trySpawn(index, store, archetypeChunk, spawnerComponent);
            if (spawnResult.ok) {
                resetSpawnerVars(spawnerComponent);
            } else {
                MobFarmingToolsPlugin.LOGGER.atWarning().log("TrySpawn failed: " + spawnResult.msg());
                spawnerComponent.incrementFailedTries(1);
            }
        }
    }

    private @NonNull SpawnResult trySpawn(
            int index,
            Store<ChunkStore> store,
            ArchetypeChunk<ChunkStore> archetypeChunk,
            MobSpawnerComponent spawnerComponent
    ) {
        World world = store.getExternalData().getWorld();
        Store<EntityStore> entityStore = world.getEntityStore().getStore();

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) { return new SpawnResult(false, "BlockStateInfo not found"); }

        Vector3d worldPos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (worldPos == null) { return new SpawnResult(false, "WorldPos (MFTBlockUtil) not found"); }

        String entityId = spawnerComponent.getEntityId();
        Vector3i entitySize = spawnerComponent.getEntitySize();

        Vector3d spawnPos = findEntitySpawnLocation(world, worldPos, entitySize.toVector3d());
        if (spawnPos == null) { return new SpawnResult(false, "EntitySpawnLocation not found"); }

        NPCPlugin.get().spawnNPC(entityStore, entityId, null, spawnPos, new Vector3f());
        return new SpawnResult(true, "All goodie");
    }

    private @Nullable Vector3d findEntitySpawnLocation(World world, Vector3d blockWorldPos, Vector3d entitySize) {
        int baseX = (int) blockWorldPos.x;
        int baseY = (int) blockWorldPos.y + 1; // spawn one above spawner
        int baseZ = (int) blockWorldPos.z;

        List<Vector3d> availableLocs = new ArrayList<>();

        int radius = 3; // 7x7 around spawner
        for (int dy = 0; dy <= entitySize.y+1; dy++) { // try same height and +entitySize.y
            for (int dz = -radius; dz <= radius+1; dz++) {
                for (int dx = -radius; dx <= radius+1; dx++) {
                    Vector3i vec = new Vector3i(baseX + dx, baseY + dy, baseZ + dz);

                    if (MFTEntityUtil.WillEntityFit(world, vec, entitySize.toVector3i())) {
                        availableLocs.add(vec.toVector3d());
                    }
                }
            }
        }

        if (availableLocs.isEmpty()) {
            return null;
        }

        int locIndex = ThreadLocalRandom.current().nextInt(0, availableLocs.size()-1);
        return availableLocs.get(locIndex);
    }

    private void resetSpawnerVars(MobSpawnerComponent spawnerComponent) {
        spawnerComponent.setLifetime(0);
        spawnerComponent.setFailedTries(0);
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
