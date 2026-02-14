package com.bryce.mobfarmtools.vacuumhopper.ui;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerConstants;
import com.bryce.mobfarmtools.util.MFTPreviewUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperConstants;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class VacuumHopperUpgradePage extends MachineUpgradePage {
    public VacuumHopperUpgradePage(PlayerRef playerRef, Ref<ChunkStore> vacuumHopperRef, BlockPosition blockPosition, int rotationIndex) {
        super(playerRef, vacuumHopperRef, MachineUpgradePage.MachineUpgradePageConfig.builder("Vacuum Hopper Upgrades", blockPosition, rotationIndex)
                .enableUpgrade(MachineUpgradeType.CHUNK_LOADING, 1)
                .enableUpgrade(MachineUpgradeType.WIDTH, VacuumHopperConstants.SIZE_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.HEIGHT, VacuumHopperConstants.SIZE_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.LENGTH, VacuumHopperConstants.SIZE_UPGRADE_MAX)
                .withPreview((context, enabled) -> {
                    if (!enabled) {
                        context.getPlayerRef().getPacketHandler().write(new ClearDebugShapes());
                        return;
                    }
                    VacuumHopperComponent hopper = getVacuumHopperComponent(context.getMachineRef());
                    if (hopper == null) {
                        return;
                    }
                    MFTPreviewUtil.ShowBoxPreviewFromMiddle(
                            context.getPlayerRef(),
                            context.getBlockPosition(),
                            context.getRotationIndex(),
                            hopper.getLength(),
                            hopper.getWidth(),
                            hopper.getHeight()
                    );
                })
                .addStatistic(VacuumHopperConstants.UpgradePageStat.ENABLED)
                .addStatistic(VacuumHopperConstants.UpgradePageStat.CHUNK_LOADED)
                .addStatistic(VacuumHopperConstants.UpgradePageStat.NOISE_SUPPRESSED)
                .addStatistic(VacuumHopperConstants.UpgradePageStat.LENGTH)
                .addStatistic(VacuumHopperConstants.UpgradePageStat.WIDTH)
                .addStatistic(VacuumHopperConstants.UpgradePageStat.HEIGHT)
                .onBeforePageOpen(context -> {
                    Ref<ChunkStore> ref = context.getMachineRef();
                    VacuumHopperComponent hopper = ref.getStore().getComponent(ref, VacuumHopperComponent.getComponentType());
                    if (hopper != null) {
                        MachineUpgradePage page = context.getPage();
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.NOISE_SUPPRESSED.getIndex(), String.valueOf(hopper.isNoiseSuppressed()));
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.CHUNK_LOADED.getIndex(), String.valueOf(hopper.isChunkLoaded()));
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.ENABLED.getIndex(), String.valueOf(hopper.isEnabled()));
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.LENGTH.getIndex(), String.valueOf(hopper.getLength()));
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.WIDTH.getIndex(), String.valueOf(hopper.getWidth()));
                        page.updateStatisticValue(VacuumHopperConstants.UpgradePageStat.HEIGHT.getIndex(), String.valueOf(hopper.getHeight()));
                    }
                })
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    VacuumHopperComponent hopper = getVacuumHopperComponent(context.getMachineRef());
                    if (hopper == null) return;

                    if (type == MachineUpgradeType.LENGTH) {
                        hopper.setLengthUpgrades(newCount);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                VacuumHopperConstants.UpgradePageStat.LENGTH.getIndex(),
                                String.valueOf(hopper.getLength())
                        );
                    } else if (type == MachineUpgradeType.WIDTH) {
                        hopper.setWidthUpgrades(newCount);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                VacuumHopperConstants.UpgradePageStat.WIDTH.getIndex(),
                                String.valueOf(hopper.getWidth())
                        );
                    } else if (type == MachineUpgradeType.HEIGHT) {
                        hopper.setHeightUpgrades(newCount);
                        MachineUpgradePage.pushStatisticValue(
                                context.getMachineRef(),
                                VacuumHopperConstants.UpgradePageStat.HEIGHT.getIndex(),
                                String.valueOf(hopper.getHeight())
                        );
                    }  else if (type == MachineUpgradeType.CHUNK_LOADING) {
                        //TODO: enable chunk loading
                    }

                    Store<ChunkStore> store = context.getMachineRef().getStore();
                    store.putComponent(context.getMachineRef(), VacuumHopperComponent.getComponentType(), hopper);
                })
                .build());
    }

    public static void clearAllPreviews(World world) {
        MachineUpgradePage.clearAllPreviews(world);
    }

    @Nullable
    private static VacuumHopperComponent getVacuumHopperComponent(Ref<ChunkStore> vacuumRef) {
        if (vacuumRef == null || !vacuumRef.isValid()) {
            return null;
        }
        return vacuumRef.getStore().getComponent(vacuumRef, VacuumHopperComponent.getComponentType());
    }
}