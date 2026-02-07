package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobSpawnerInitializer extends RefSystem<ChunkStore> {

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobSpawnerComponent spawnerComponent = store.getComponent(ref, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log("Could not find spawner component on spawner added.");
            return;
        }

        MobFarmingToolsPlugin.LOGGER.atInfo().log("[[SPAWNER]] RateMin: "+spawnerComponent.getSpawnRateMin());
        MobFarmingToolsPlugin.LOGGER.atInfo().log("[[SPAWNER]] RateMax: "+spawnerComponent.getSpawnRateMax());
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                MobSpawnerComponent.getComponentType()
        );
    }
}
