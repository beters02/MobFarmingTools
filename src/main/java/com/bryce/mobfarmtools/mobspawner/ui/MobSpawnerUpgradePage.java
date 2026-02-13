package com.bryce.mobfarmtools.mobspawner.ui;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class MobSpawnerUpgradePage extends MachineUpgradePage {
    public MobSpawnerUpgradePage(PlayerRef playerRef, Ref<ChunkStore> mobSpawnerRef, BlockPosition blockPosition, int rotationIndex) {
        super(playerRef, mobSpawnerRef, MachineUpgradePage.MachineUpgradePageConfig.builder("Mob Spawner Upgrades", blockPosition, rotationIndex)
                .enableUpgrade(MachineUpgradeType.CHUNK_LOADING, 1)
                .enableUpgrade(MachineUpgradeType.OUTPUT, 2)
                .enableUpgrade(MachineUpgradeType.SPEED, 4)
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    MobSpawnerComponent mobSpawner = getMobSpawnerComponent(context.getMachineRef());
                    if (mobSpawner == null) {
                        return;
                    }

                    if (type == MachineUpgradeType.OUTPUT) {
                        mobSpawner.setSpawnAmountMin(mobSpawner.getSpawnAmountMinFromOutputUpgrade(newCount));
                        mobSpawner.setSpawnAmountMax(mobSpawner.getSpawnAmountMaxFromOutputUpgrade(newCount));
                    } else if (type == MachineUpgradeType.SPEED) {
                        mobSpawner.setSpawnRateMin(mobSpawner.getSpawnRateMinFromSpeedUpgrade(newCount));
                        mobSpawner.setSpawnRateMax(mobSpawner.getSpawnRateMaxFromSpeedUpgrade(newCount));
                    } else if (type == MachineUpgradeType.CHUNK_LOADING) {
                        //TODO: enable chunk loading
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
