package com.bryce.mobfarmtools.mobmasher.ui;

import com.bryce.mobfarmtools.chunks.ForcedChunkPersistence;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.mobmasher.MobMasherComponent;
import com.bryce.mobfarmtools.mobmasher.MobMasherConstants;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterComponent;
import com.bryce.mobfarmtools.util.MFTPreviewUtil;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public final class MobMasherUpgradePage extends MachineUpgradePage {
    public MobMasherUpgradePage(PlayerRef playerRef, Ref<ChunkStore> mobFanRef, BlockPosition blockPosition, int rotationIndex) {
        super(playerRef, mobFanRef, MachineUpgradePageConfig.builder("Mob Fan Upgrades", blockPosition, rotationIndex)
                .enableUpgrade(MachineUpgradeType.CHUNK_LOADING, 1)
                .enableUpgrade(MachineUpgradeType.SPEED, MobMasherConstants.SPEED_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.OUTPUT, MobMasherConstants.OUTPUT_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.NOISE_SUPPRESSION, 1)
                .addStatistic(MobMasherConstants.UpgradePageStat.ENABLED)
                .addStatistic(MobMasherConstants.UpgradePageStat.CHUNK_LOADED)
                .addStatistic(MobMasherConstants.UpgradePageStat.NOISE_SUPPRESSED)
                .addStatistic(MobMasherConstants.UpgradePageStat.SPEED)
                .addStatistic(MobMasherConstants.UpgradePageStat.OUTPUT)
                .addStatistic(MobMasherConstants.UpgradePageStat.DAMAGE_BOSSES_ENABLED)
                .onBeforePageOpen(context -> {
                    MobMasherComponent mobMasher = getMobMasherComponent(context.getMachineRef());
                    if (mobMasher != null) {
                        MachineUpgradePage page = context.getPage();
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(), String.valueOf(mobMasher.isNoiseSuppressed()));
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.CHUNK_LOADED.getIndex(), String.valueOf(mobMasher.isChunkLoaded()));
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.ENABLED.getIndex(), String.valueOf(mobMasher.isEnabled()));
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.SPEED.getIndex(), String.valueOf(mobMasher.getTicksPerAction()/30));
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.OUTPUT.getIndex(), String.valueOf(mobMasher.getDamagePerAction()));
                        page.updateStatisticValue(MobMasherConstants.UpgradePageStat.DAMAGE_BOSSES_ENABLED.getIndex(), String.valueOf(mobMasher.isDamageBossesEnabled()));
                    }
                })
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    MobMasherComponent mobMasher = getMobMasherComponent(context.getMachineRef());
                    if (mobMasher == null) return;

                    if (type == MachineUpgradeType.OUTPUT) {
                        int damage = getDamage(newCount);
                        boolean damageBossesEnabled = getDamageBossesEnabled(newCount);
                        mobMasher.setDamagePerAction(damage);
                        mobMasher.setDamageBossesEnabled(damageBossesEnabled);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobMasherConstants.UpgradePageStat.OUTPUT.getIndex(),
                                String.valueOf(mobMasher.getDamagePerAction())
                        );
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobMasherConstants.UpgradePageStat.DAMAGE_BOSSES_ENABLED.getIndex(),
                                String.valueOf(mobMasher.isDamageBossesEnabled())
                        );
                    } else if (type == MachineUpgradeType.SPEED) {
                        int speed = getSpeed(newCount);
                        mobMasher.setTicksPerAction(speed);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobMasherConstants.UpgradePageStat.SPEED.getIndex(),
                                String.valueOf(mobMasher.getTicksPerAction())
                        );
                    }  else if (type == MachineUpgradeType.CHUNK_LOADING) {
                        mobMasher.setChunkLoaded(newCount > 0);
                        World world = context.getMachineRef().getStore().getExternalData().getWorld();
                        ForcedChunkPersistence.setForced(world, blockPosition, newCount > 0);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobMasherConstants.UpgradePageStat.CHUNK_LOADED.getIndex(),
                                String.valueOf(mobMasher.isChunkLoaded())
                        );
                    } else if (type == MachineUpgradeType.NOISE_SUPPRESSION) {
                        mobMasher.setNoiseSuppressed(newCount > 0);
                        MFTSoundEmitterComponent emitter = context.getMachineRef().getStore().getComponent(context.getMachineRef(), MFTSoundEmitterComponent.getComponentType());
                        if (emitter != null) {
                            emitter.setSuppressed(newCount > 0, context.getMachineRef());
                        }
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                MobMasherConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(),
                                String.valueOf(mobMasher.isNoiseSuppressed())
                        );
                    }

                    Store<ChunkStore> store = context.getMachineRef().getStore();
                    store.putComponent(context.getMachineRef(), MobMasherComponent.getComponentType(), mobMasher);
                })
                .build());
    }

    private static int getDamage(int newCount) {
        if (newCount == 1) {
            return MobMasherConstants.UPG1_DAMAGE;
        } else if (newCount == 2) {
            return MobMasherConstants.UPG2_DAMAGE;
        } else if (newCount == 3) {
            return MobMasherConstants.UPG3_DAMAGE;
        } else if (newCount == 4) {
            return MobMasherConstants.UPG4_DAMAGE;
        }
        return MobMasherConstants.DEF_DAMAGE;
    }

    private static int getSpeed(int newCount) {
        if (newCount == 1) {
            return MobMasherConstants.UPG1_SPEED;
        } else if (newCount == 2) {
            return MobMasherConstants.UPG2_SPEED;
        }
        return MobMasherConstants.DEF_TICKS_PER_ACTION;
    }

    private static boolean getDamageBossesEnabled(int newCount) {
        return newCount >= MobMasherConstants.OUTPUT_AMOUNT_REQUIRED_FOR_DAMAGE_BOSSES;
    }

    @Nullable
    private static MobMasherComponent getMobMasherComponent(Ref<ChunkStore> mobMasherRef) {
        if (mobMasherRef == null || !mobMasherRef.isValid()) {
            return null;
        }
        return mobMasherRef.getStore().getComponent(mobMasherRef, MobMasherComponent.getComponentType());
    }
}
