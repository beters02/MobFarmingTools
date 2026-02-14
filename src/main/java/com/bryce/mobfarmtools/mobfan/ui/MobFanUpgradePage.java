package com.bryce.mobfarmtools.mobfan.ui;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerConstants;
import com.bryce.mobfarmtools.util.MFTPreviewUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperConstants;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public final class MobFanUpgradePage extends MachineUpgradePage {
    public MobFanUpgradePage(PlayerRef playerRef, Ref<ChunkStore> mobFanRef, BlockPosition blockPosition, int rotationIndex) {
        super(playerRef, mobFanRef, MachineUpgradePage.MachineUpgradePageConfig.builder("Mob Fan Upgrades", blockPosition, rotationIndex)
                .enableUpgrade(MachineUpgradeType.CHUNK_LOADING, 1)
                .enableUpgrade(MachineUpgradeType.WIDTH, MobFanConstants.FAN_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.HEIGHT, MobFanConstants.FAN_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.LENGTH, MobFanConstants.FAN_UPGRADE_MAX)
                .addStatistic(MobFanConstants.UpgradePageStat.ENABLED)
                .addStatistic(MobFanConstants.UpgradePageStat.CHUNK_LOADED)
                .addStatistic(MobFanConstants.UpgradePageStat.NOISE_SUPPRESSED)
                .addStatistic(MobFanConstants.UpgradePageStat.LENGTH)
                .addStatistic(MobFanConstants.UpgradePageStat.WIDTH)
                .addStatistic(MobFanConstants.UpgradePageStat.HEIGHT)
                .onBeforePageOpen(context -> {
                    Ref<ChunkStore> ref = context.getMachineRef();
                    MobFanComponent fan = ref.getStore().getComponent(ref, MobFanComponent.getComponentType());
                    if (fan != null) {
                        MachineUpgradePage page = context.getPage();
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(), String.valueOf(fan.isNoiseSuppressed()));
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.CHUNK_LOADED.getIndex(), String.valueOf(fan.isChunkLoaded()));
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.ENABLED.getIndex(), String.valueOf(fan.isEnabled()));
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.LENGTH.getIndex(), String.valueOf(fan.getFanLength()));
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.WIDTH.getIndex(), String.valueOf(fan.getFanWidth()));
                        page.updateStatisticValue(MobFanConstants.UpgradePageStat.HEIGHT.getIndex(), String.valueOf(fan.getFanHeight()));
                    }
                })
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    MobFanComponent mobFan = getMobFanComponent(context.getMachineRef());
                    if (mobFan == null) {
                        return;
                    }

                    switch (type) {
                        case WIDTH -> {
                            mobFan.setWidthUpgrades(newCount);
                            MachineUpgradePage.pushStatisticValue(
                                    context.getMachineRef(),
                                    MobFanConstants.UpgradePageStat.WIDTH.getIndex(),
                                    String.valueOf(mobFan.getFanWidth())
                            );
                        }
                        case HEIGHT -> {
                            mobFan.setHeightUpgrades(newCount);
                            MachineUpgradePage.pushStatisticValue(
                                    context.getMachineRef(),
                                    MobFanConstants.UpgradePageStat.HEIGHT.getIndex(),
                                    String.valueOf(mobFan.getFanHeight())
                            );
                        }
                        case LENGTH -> {
                            mobFan.setLengthUpgrades(newCount);
                            MachineUpgradePage.pushStatisticValue(
                                    context.getMachineRef(),
                                    MobFanConstants.UpgradePageStat.LENGTH.getIndex(),
                                    String.valueOf(mobFan.getFanLength())
                            );
                        }
                        default -> {
                            return;
                        }
                    }

                    Store<ChunkStore> store = context.getMachineRef().getStore();
                    store.putComponent(context.getMachineRef(), MobFanComponent.getComponentType(), mobFan);
                })
                .withPreview((context, enabled) -> {
                    if (!enabled) {
                        context.getPlayerRef().getPacketHandler().write(new ClearDebugShapes());
                        return;
                    }
                    MobFanComponent mobFan = getMobFanComponent(context.getMachineRef());
                    if (mobFan == null) {
                        return;
                    }
                    MFTPreviewUtil.ShowBoxPreview(
                            context.getPlayerRef(),
                            context.getBlockPosition(),
                            context.getRotationIndex(),
                            mobFan.getFanLength(),
                            mobFan.getFanWidth(),
                            mobFan.getFanHeight(),
                            mobFan.getBaseForward()
                    );
                })
                .build());
    }

    public static void clearAllPreviews(World world) {
        MachineUpgradePage.clearAllPreviews(world);
    }

    @Nullable
    private static MobFanComponent getMobFanComponent(Ref<ChunkStore> mobFanRef) {
        if (mobFanRef == null || !mobFanRef.isValid()) {
            return null;
        }
        return mobFanRef.getStore().getComponent(mobFanRef, MobFanComponent.getComponentType());
    }
}
