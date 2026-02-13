package com.bryce.mobfarmtools.mobfan.ui;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.util.MFTPreviewUtil;
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
                .enableUpgrade(MachineUpgradeType.WIDTH, MobFanConstants.FAN_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.HEIGHT, MobFanConstants.FAN_UPGRADE_MAX)
                .enableUpgrade(MachineUpgradeType.LENGTH, MobFanConstants.FAN_UPGRADE_MAX)
                .onUpgradeChanged((context, type, oldCount, newCount) -> {
                    MobFanComponent mobFan = getMobFanComponent(context.getMachineRef());
                    if (mobFan == null) {
                        return;
                    }

                    switch (type) {
                        case WIDTH -> mobFan.setWidthUpgrades(newCount);
                        case HEIGHT -> mobFan.setHeightUpgrades(newCount);
                        case LENGTH -> mobFan.setLengthUpgrades(newCount);
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
