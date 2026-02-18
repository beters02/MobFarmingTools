package com.bryce.mobfarmtools.mobspawner.ui;

import com.bryce.mobfarmtools.chunks.ForcedChunkPersistence;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerConstants;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerInitializer;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterComponent;
import com.bryce.mobfarmtools.util.MFTChunkUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class MobSpawnerUpgradePage extends MachineUpgradePage {
    public MobSpawnerUpgradePage(PlayerRef playerRef, Ref<ChunkStore> mobSpawnerRef, BlockPosition blockPosition, int rotationIndex) {

        super(playerRef, mobSpawnerRef, MachineUpgradePage.MachineUpgradePageConfig.builder("Mob Spawner Upgrades", blockPosition, rotationIndex)
                .enableUpgrade(MachineUpgradeType.CHUNK_LOADING, 1)
                .enableUpgrade(MachineUpgradeType.OUTPUT, 2)
                .enableUpgrade(MachineUpgradeType.SPEED, 4)
                .enableUpgrade(MachineUpgradeType.NOISE_SUPPRESSION, 1)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.ENTITY_ID)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.ENABLED)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.CHUNK_LOADED)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.NOISE_SUPPRESSED)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.SPAWN_RATE)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.SPAWN_AMOUNT)
                .addStatistic(MobSpawnerConstants.UpgradePageStat.NEXT_SPAWN_TIME)
                .onBeforePageOpen(context -> {
                    Ref<ChunkStore> ref = context.getMachineRef();
                    MobSpawnerComponent spawner = ref.getStore().getComponent(ref, MobSpawnerComponent.getComponentType());
                    if (spawner != null) {
                        MachineUpgradePage page = context.getPage();
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.ENTITY_ID.getIndex(), spawner.getEntityId());
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(), String.valueOf(spawner.isNoiseSuppressed()));
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.CHUNK_LOADED.getIndex(), String.valueOf(spawner.isChunkLoaded()));
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.SPAWN_RATE.getIndex(), spawner.getStatValueSpawnRate());
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.SPAWN_AMOUNT.getIndex(), spawner.getStatValueSpawnAmount());
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.ENABLED.getIndex(), String.valueOf(spawner.isEnabled()));
                        page.updateStatisticValue(MobSpawnerConstants.UpgradePageStat.NEXT_SPAWN_TIME.getIndex(), String.valueOf(spawner.getTimeUntilNextSpawn()));
                    }
                })
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    MobSpawnerComponent mobSpawner = getMobSpawnerComponent(context.getMachineRef());
                    if (mobSpawner == null) {
                        return;
                    }

                    if (type == MachineUpgradeType.OUTPUT) {
                        mobSpawner.setSpawnAmountMin(mobSpawner.getSpawnAmountMinFromOutputUpgrade(newCount));
                        mobSpawner.setSpawnAmountMax(mobSpawner.getSpawnAmountMaxFromOutputUpgrade(newCount));
                        mobSpawner.onSpawnAmountChanged();
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobSpawnerConstants.UpgradePageStat.SPAWN_AMOUNT.getIndex(),
                                mobSpawner.getStatValueSpawnAmount()
                        );
                    } else if (type == MachineUpgradeType.SPEED) {
                        mobSpawner.setSpawnRateMin(mobSpawner.getSpawnRateMinFromSpeedUpgrade(newCount));
                        mobSpawner.setSpawnRateMax(mobSpawner.getSpawnRateMaxFromSpeedUpgrade(newCount));
                        mobSpawner.onSpawnRateChanged();
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobSpawnerConstants.UpgradePageStat.SPAWN_RATE.getIndex(),
                                mobSpawner.getStatValueSpawnRate()
                        );
                    } else if (type == MachineUpgradeType.CHUNK_LOADING) {
                        mobSpawner.setChunkLoaded(newCount > 0);
                        World world = context.getMachineRef().getStore().getExternalData().getWorld();
                        ForcedChunkPersistence.setForced(world, blockPosition, newCount > 0);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobSpawnerConstants.UpgradePageStat.CHUNK_LOADED.getIndex(),
                                String.valueOf(mobSpawner.isChunkLoaded())
                        );
                    } else if (type == MachineUpgradeType.NOISE_SUPPRESSION) {
                        mobSpawner.setNoiseSuppressed(newCount > 0);
                        MFTSoundEmitterComponent emitter = context.getMachineRef().getStore().getComponent(context.getMachineRef(), MFTSoundEmitterComponent.getComponentType());
                        if (emitter != null) {
                            emitter.setSuppressed(newCount > 0, context.getMachineRef());
                        }
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobSpawnerConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(),
                                String.valueOf(mobSpawner.isNoiseSuppressed())
                        );
                    }

                    Store<ChunkStore> store = context.getMachineRef().getStore();
                    store.putComponent(context.getMachineRef(), MobSpawnerComponent.getComponentType(), mobSpawner);
                })
                .build());
    }

    public static void clearAllPreviews(World world) {
        MachineUpgradePage.clearAllPreviews(world);
    }

    @Nullable
    private static MobSpawnerComponent getMobSpawnerComponent(Ref<ChunkStore> mobSpawnerRef) {
        if (mobSpawnerRef == null || !mobSpawnerRef.isValid()) {
            return null;
        }
        return mobSpawnerRef.getStore().getComponent(mobSpawnerRef, MobSpawnerComponent.getComponentType());
    }
}
