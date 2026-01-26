package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MobFanComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MobFanComponent> CODEC =
            BuilderCodec.builder(
                    MobFanComponent.class,
                    MobFanComponent::new
            ).build();

    private final float FAN_SPEED = 100f;

    private int fanLength = 3;
    private int fanWidth = 3;
    private int fanHeight = 3;
    private boolean enabled = false;

    private World _world;
    private Vector3i _worldPos;
    private Vector3d baseForward = new Vector3d(0, 0, -1);

    public void setFanLength(int amount) {
        if (amount > MobFanConstants.FAN_LENGTH_MAX) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN LENGTH CAN NOT BE GREATER THAN "+MobFanConstants.FAN_LENGTH_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_LENGTH_MIN) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN LENGTH CAN NOT BE LESS THAN "+ MobFanConstants.FAN_LENGTH_MIN);
            return;
        }

        fanLength = amount;
    }

    public void setFanWidth(int amount) {
        if (amount > MobFanConstants.FAN_WIDTH_MAX) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN WIDTH CAN NOT BE GREATER THAN "+MobFanConstants.FAN_WIDTH_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_WIDTH_MIN) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN WIDTH CAN NOT BE LESS THAN "+MobFanConstants.FAN_WIDTH_MIN);
            return;
        }

        fanWidth = amount;
    }

    public void setFanHeight(int amount) {
        if (amount > MobFanConstants.FAN_HEIGHT_MAX) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN HEIGHT CAN NOT BE GREATER THAN "+MobFanConstants.FAN_HEIGHT_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_HEIGHT_MIN) {
            MobFarmingToolsPlugin.LOGGER.atSevere().log("FAN HEIGHT CAN NOT BE LESS THAN "+MobFanConstants.FAN_HEIGHT_MIN);
            return;
        }

        fanHeight = amount;
    }

    public void setEnabled(boolean enabled) {
        if (this._worldPos == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log(
                    "MobFanComponent Stored WorldPos is null. Block interaction state cannot be changed.");
            return;
        }

        if (this._world == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log(
                    "MobFanComponent Stored World is null. Block interaction state cannot be changed.");
            return;
        }

        BlockType blockType = this._world.getBlockType(this._worldPos);

        if (blockType == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log(
                    "MobFanComponent BlockType returned null. Block interaction state cannot be changed.");
            return;
        }

        this.enabled = enabled;
        this._world.setBlockInteractionState(this._worldPos, blockType, enabled ? "On" : "Off");

        MobFarmingToolsPlugin.LOGGER.atInfo().log(
                "MobFanComponent Interaction state successfully set to " + (enabled ? "On" : "Off"));
    }

    public void setStoredWorld(World world) { this._world = world; }
    public void setStoredWorldPos(Vector3i pos) { this._worldPos = pos; }
    public void setBaseForward(Vector3d forward) { this.baseForward = forward; }

    public void incrementFanLength(int amount) {
        setFanLength(fanLength + amount);
    }
    public void incrementFanWidth(int amount) {
        setFanWidth(fanWidth + amount);
    }
    public void incrementFanHeight(int amount) {
        setFanHeight(fanHeight + amount);
    }

    public final int getFanLength() { return fanLength; }
    public final int getFanWidth() { return fanWidth; }
    public final int getFanHeight() { return fanHeight; }
    public final boolean isEnabled() { return this.enabled; }
    public final World getStoredWorld() { return this._world; }
    public final Vector3i getStoredWorldPos() { return this._worldPos; }
    public final Vector3d getBaseForward() { return this.baseForward; }

    public void tickAction(float dt, int globalX, int globalY, int globalZ, int rotationIndex, World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        List<Ref<EntityStore>> hits = getEntitiesInFanBox(globalX, globalY, globalZ, rotationIndex, store);

        hits.forEach(ref -> {
            if (ref == null || !ref.isValid()) {
                MobFarmingToolsPlugin.LOGGER.atWarning().log("Attempted to tick action but Ref<EntityStore> is null or invalid.");
                return;
            }

            Velocity velocityComponent = store.getComponent(ref, Velocity.getComponentType());

            if (velocityComponent == null) {
                MobFarmingToolsPlugin.LOGGER.atWarning().log("Velocity component is null.");
                return;
            }

            Vector3d push = getForwardDirection(rotationIndex);
            push = new Vector3d(
                    push.x * (FAN_SPEED * dt),
                    push.y * (FAN_SPEED * dt),
                    push.z * (FAN_SPEED * dt)
            );

            velocityComponent.addInstruction(push, new VelocityConfig(), ChangeVelocityType.Add);
        });
    }

    private Vector3d getForwardDirection(int rotationIndex) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        return Rotation.rotate(this.baseForward, rot.yaw(), rot.pitch(), rot.roll()).normalize();
    }

    private List<Ref<EntityStore>> getEntitiesInFanBox(int x, int y, int z, int rotationIndex, Store<EntityStore> entityStore) {
        Vector3d forward = getForwardDirection(rotationIndex);
        Vector3d blockCenter = new Vector3d(x + 0.5, y + 0.5, z + 0.5);

        double length = this.fanLength;
        double width = this.fanWidth;
        double height = this.fanHeight;
        double start = 0.5;

        Vector3d boxCenter = blockCenter.clone().add(forward.clone().scale(start + length * 0.5));

        Vector3d min = new Vector3d(
                boxCenter.x - width * 0.5,
                boxCenter.y - height * 0.5,
                boxCenter.z - length * 0.5
        );
        Vector3d max = new Vector3d(
                boxCenter.x + width * 0.5,
                boxCenter.y + height * 0.5,
                boxCenter.z + length * 0.5
        );

        return TargetUtil.getAllEntitiesInBox(min, max, entityStore);
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        return new MobFanComponent();
    }
}
