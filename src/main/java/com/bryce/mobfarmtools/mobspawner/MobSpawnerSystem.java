package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTEntityUtil;
import com.bryce.mobfarmtools.util.MFTMathUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
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

    private record SpawnResult(boolean ok, String msg) {}

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobSpawnerComponent spawnerComponent = archetypeChunk.getComponent(index, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) return;
        if (!spawnerComponent.canTick()) return;

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
                MobFarmingToolsPlugin.LOGGER.atInfo().log(spawnResult.msg());
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
        if (info == null) return new SpawnResult(false, "BlockStateInfo not found");

        Vector3d worldPos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (worldPos == null) return new SpawnResult(false, "WorldPos (MFTBlockUtil) not found");

        // Skip first try if max entities are met
        Box box = MFTMathUtil.GetBoxFromPosition(worldPos, MobSpawnerConstants.SPAWN_RADIUS);
        int entitiesCount = TargetUtil.getAllEntitiesInBox(box.min, box.max, entityStore).size();
        if (entitiesCount > spawnerComponent.getMaxEntities()) return new SpawnResult(true, "Skipped for max entities.");

        Vector3d spawnPos = findEntitySpawnLocation(world, worldPos, spawnerComponent.getEntitySize().toVector3d());
        if (spawnPos == null) return new SpawnResult(false, "EntitySpawnLocation not found");

        spawnerComponent.spawnAction(entityStore, spawnPos);
        return new SpawnResult(true, "Spawn action fired.");
    }

    private @Nullable Vector3d findEntitySpawnLocation(World world, Vector3d blockWorldPos, Vector3d entitySize) {
        int baseX = (int) blockWorldPos.x;
        int baseY = (int) blockWorldPos.y + 1;
        int baseZ = (int) blockWorldPos.z;

        List<Vector3d> availableAirLocs = new ArrayList<>();
        List<Vector3d> availableGroundLocs = new ArrayList<>();

        int radius = (int) MobSpawnerConstants.SPAWN_RADIUS; // 7x7 around spawner
        for (int dy = 0; dy <= entitySize.y+1; dy++) { // try same height and +entitySize.y
            for (int dz = -radius; dz <= radius+1; dz++) {
                for (int dx = -radius; dx <= radius+1; dx++) {
                    Vector3i spawnLoc = new Vector3i(baseX + dx, baseY + dy, baseZ + dz);

                    if (MFTEntityUtil.WillEntityFit(world, spawnLoc, entitySize.toVector3i())) {
                        if (spawnLoc.y == baseY) {
                            availableGroundLocs.add(spawnLoc.toVector3d());
                        } else {
                            availableAirLocs.add(spawnLoc.toVector3d());
                        }
                    }
                }
            }
        }

        if (availableAirLocs.isEmpty() && availableGroundLocs.isEmpty()) return null;

        // prefer ground location.
        // if there is less than 5 ground locations, the spawning won't look random enough. so we combine the tables
        if (availableGroundLocs.size() < 5) {
            availableGroundLocs.addAll(availableAirLocs);
        }

        return availableGroundLocs.get(MFTMathUtil.RandomRange(0, availableGroundLocs.size()-1));
    }

    private void resetSpawnerVars(MobSpawnerComponent spawnerComponent) {
        spawnerComponent.setLifetime(0);
        spawnerComponent.setFailedTries(0);
        spawnerComponent.setRandomSpawnRate();
        spawnerComponent.setRandomSpawnAmount();
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.mobSpawnerComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }
}
