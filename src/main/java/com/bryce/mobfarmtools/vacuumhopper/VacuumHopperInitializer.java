package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VacuumHopperInitializer extends RefSystem<ChunkStore> {
    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                VacuumHopperComponent.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        VacuumHopperComponent vacuumHopperComponent = store.getComponent(ref, VacuumHopperComponent.getComponentType());
        if (vacuumHopperComponent != null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log("Vacuum Hopper component found on init.");
        }
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {

    }
}
