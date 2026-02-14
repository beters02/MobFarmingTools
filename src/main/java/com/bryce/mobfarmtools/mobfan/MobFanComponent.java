package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTVectorUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
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
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MobFanComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MobFanComponent> CODEC =
            BuilderCodec.builder(
                            MobFanComponent.class,
                            MobFanComponent::new
                    )
                    .append(new KeyedCodec<>("LengthUpgrades", Codec.INTEGER),
                            (component, value) -> component.lengthUpgrades = value,
                            component -> component.lengthUpgrades)
                    .add()
                    .append(new KeyedCodec<>("WidthUpgrades", Codec.INTEGER),
                            (component, value) -> component.widthUpgrades = value,
                            component -> component.widthUpgrades)
                    .add()
                    .append(new KeyedCodec<>("HeightUpgrades", Codec.INTEGER),
                            (component, value) -> component.heightUpgrades = value,
                            component -> component.heightUpgrades)
                    .add()
                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (component, value) -> component.enabled = value,
                            component -> component.enabled)
                    .add()
                    .append(new KeyedCodec<>("ChunkLoaded", Codec.BOOLEAN),
                            (component, value) -> component.chunkLoaded = value,
                            component -> component.chunkLoaded)
                    .add()
                    .append(new KeyedCodec<>("NoiseSuppressed", Codec.BOOLEAN),
                            (component, value) -> component.noiseSuppressed = value,
                            component -> component.noiseSuppressed)
                    .add()
                    .afterDecode(MobFanComponent::applyUpgradeCounts)
                    .build();

    private final float FAN_SPEED = 100f;

    private int fanLength = 3;
    private int fanWidth = 3;
    private int fanHeight = 3;
    private int lengthUpgrades = 0;
    private int widthUpgrades = 0;
    private int heightUpgrades = 0;
    private boolean enabled = true;
    private boolean noiseSuppressed = false;
    private boolean chunkLoaded = false;
    private transient SimpleItemContainer upgradeContainer;

    private World _world;
    private Vector3i _worldPos;
    private Vector3d baseForward = new Vector3d(0, 0, -1);

    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobFanComponent]");

    @NonNull
    public static ComponentType<ChunkStore, MobFanComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMobFanComponentType();
    }

    public MobFanComponent() {
        debugger.setEnabled(false);
    }

    public void setFanLength(int amount) {
        if (amount > MobFanConstants.FAN_LENGTH_MAX) {
            debugger.atSevere("FAN LENGTH CAN NOT BE GREATER THAN " + MobFanConstants.FAN_LENGTH_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_LENGTH_MIN) {
            debugger.atSevere("FAN LENGTH CAN NOT BE LESS THAN " + MobFanConstants.FAN_LENGTH_MIN);
            return;
        }

        fanLength = amount;
    }

    public void setFanWidth(int amount) {
        if (amount > MobFanConstants.FAN_WIDTH_MAX) {
            debugger.atSevere("FAN WIDTH CAN NOT BE GREATER THAN " + MobFanConstants.FAN_WIDTH_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_WIDTH_MIN) {
            debugger.atSevere("FAN WIDTH CAN NOT BE LESS THAN " + MobFanConstants.FAN_WIDTH_MIN);
            return;
        }

        fanWidth = amount;
    }

    public void setFanHeight(int amount) {
        if (amount > MobFanConstants.FAN_HEIGHT_MAX) {
            debugger.atSevere("FAN HEIGHT CAN NOT BE GREATER THAN " + MobFanConstants.FAN_HEIGHT_MAX);
            return;
        } else if (amount < MobFanConstants.FAN_HEIGHT_MIN) {
            debugger.atSevere("FAN HEIGHT CAN NOT BE LESS THAN " + MobFanConstants.FAN_HEIGHT_MIN);
            return;
        }

        fanHeight = amount;
    }

    public void setEnabled(boolean enabled) {
        if (this._worldPos == null) {
            debugger.atWarning(
                    "MobFanComponent Stored WorldPos is null. Block interaction state cannot be changed.");
            return;
        }

        if (this._world == null) {
            debugger.atWarning(
                    "MobFanComponent Stored World is null. Block interaction state cannot be changed.");
            return;
        }

        BlockType blockType = this._world.getBlockType(this._worldPos);

        if (blockType == null) {
            debugger.atWarning(
                    "MobFanComponent BlockType returned null. Block interaction state cannot be changed.");
            return;
        }

        this.enabled = enabled;
        this._world.setBlockInteractionState(this._worldPos, blockType, enabled ? "On" : "Off");

        debugger.atWarning(
                "MobFanComponent Interaction state successfully set to " + (enabled ? "On" : "Off"));
    }

    public void setStoredWorld(World world) {
        this._world = world;
    }

    public void setStoredWorldPos(Vector3i pos) {
        this._worldPos = pos;
    }

    public void setBaseForward(Vector3d forward) {
        this.baseForward = forward;
    }

    public void incrementFanLength(int amount) {
        setFanLength(fanLength + amount);
    }

    public void incrementFanWidth(int amount) {
        setFanWidth(fanWidth + amount);
    }

    public void incrementFanHeight(int amount) {
        setFanHeight(fanHeight + amount);
    }

    public void setLengthUpgrades(int amount) {
        lengthUpgrades = clampUpgrade(amount);
        applyUpgradeCounts();
    }

    public void setWidthUpgrades(int amount) {
        widthUpgrades = clampUpgrade(amount);
        applyUpgradeCounts();
    }

    public void setHeightUpgrades(int amount) {
        heightUpgrades = clampUpgrade(amount);
        applyUpgradeCounts();
    }

    public final int getFanLength() {
        return fanLength;
    }

    public final int getFanWidth() {
        return fanWidth;
    }

    public final int getFanHeight() {
        return fanHeight;
    }

    public final int getLengthUpgrades() {
        return lengthUpgrades;
    }

    public final int getWidthUpgrades() {
        return widthUpgrades;
    }

    public final int getHeightUpgrades() {
        return heightUpgrades;
    }

    public final boolean isEnabled() {
        return this.enabled;
    }

    public final World getStoredWorld() {
        return this._world;
    }

    public final Vector3i getStoredWorldPos() {
        return this._worldPos;
    }

    public final Vector3d getBaseForward() {
        return this.baseForward;
    }

    public boolean isNoiseSuppressed() {
        return noiseSuppressed;
    }

    public boolean isChunkLoaded() {
        return chunkLoaded;
    }

    public void setNoiseSuppressed(boolean value) {
        noiseSuppressed = value;
    }

    public void setChunkLoaded(boolean value) {
        chunkLoaded = value;
    }

    public SimpleItemContainer getOrCreateUpgradeContainer() {
        if (upgradeContainer == null) {
            upgradeContainer = new SimpleItemContainer((short) 3);
        }
        return upgradeContainer;
    }

    public void tickAction(float dt, int globalX, int globalY, int globalZ, int rotationIndex, World world) {
        Store<EntityStore> store = world.getEntityStore().getStore();
        List<Ref<EntityStore>> hits = getEntitiesInFanBox(globalX, globalY, globalZ, rotationIndex, store);

        hits.forEach(ref -> {
            if (ref == null || !ref.isValid()) {
                debugger.atWarning("Attempted to tick action but Ref<EntityStore> is null or invalid.");
                return;
            }

            Velocity velocityComponent = store.getComponent(ref, Velocity.getComponentType());

            if (velocityComponent == null) {
                debugger.atWarning("Velocity component is null.");
                return;
            }

            Vector3d push = getForwardDirection(rotationIndex);
            MFTVectorUtil.multiply(push, FAN_SPEED * dt);

            velocityComponent.addInstruction(push, new VelocityConfig(), ChangeVelocityType.Add);
        });
    }

    private Vector3d getForwardDirection(int rotationIndex) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        return Rotation.rotate(this.baseForward, rot.yaw(), rot.pitch(), rot.roll()).normalize();
    }

    private List<Ref<EntityStore>> getEntitiesInFanBox(int x, int y, int z, int rotationIndex, Store<EntityStore> entityStore) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        Vector3d forward = getForwardDirection(rotationIndex);
        Vector3d right = Rotation.rotate(new Vector3d(1, 0, 0), rot.yaw(), rot.pitch(), rot.roll()).normalize();
        Vector3d up = Rotation.rotate(new Vector3d(0, 1, 0), rot.yaw(), rot.pitch(), rot.roll()).normalize();
        Vector3d blockCenter = new Vector3d(x + 0.5, y + 0.5, z + 0.5);

        double length = this.fanLength;
        double width = this.fanWidth;
        double height = this.fanHeight;
        double start = 0.5;

        Vector3d boxCenter = blockCenter.clone().add(forward.clone().scale(start + length * 0.5));
        Vector3d halfForward = forward.clone().scale(length * 0.5);
        Vector3d halfRight = right.clone().scale(width * 0.5);
        Vector3d halfUp = up.clone().scale(height * 0.5);

        Vector3d min = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        int[] signs = new int[]{-1, 1};
        for (int sx : signs) {
            for (int sy : signs) {
                for (int sz : signs) {
                    Vector3d corner = boxCenter.clone()
                            .add(halfRight.clone().scale(sx))
                            .add(halfUp.clone().scale(sy))
                            .add(halfForward.clone().scale(sz));
                    min.x = Math.min(min.x, corner.x);
                    min.y = Math.min(min.y, corner.y);
                    min.z = Math.min(min.z, corner.z);
                    max.x = Math.max(max.x, corner.x);
                    max.y = Math.max(max.y, corner.y);
                    max.z = Math.max(max.z, corner.z);
                }
            }
        }

        return TargetUtil.getAllEntitiesInBox(min, max, entityStore);
    }

    private void applyUpgradeCounts() {
        lengthUpgrades = clampUpgrade(lengthUpgrades);
        widthUpgrades = clampUpgrade(widthUpgrades);
        heightUpgrades = clampUpgrade(heightUpgrades);
        setFanLength(MobFanConstants.FAN_LENGTH_MIN + lengthUpgrades);
        setFanWidth(MobFanConstants.FAN_WIDTH_MIN + (widthUpgrades * 2));
        setFanHeight(MobFanConstants.FAN_HEIGHT_MIN + (heightUpgrades * 2));
    }

    private static int clampUpgrade(int amount) {
        if (amount < 0) {
            return 0;
        }
        return Math.min(amount, MobFanConstants.FAN_UPGRADE_MAX);
    }

    public void printDebug() {
        debugger.atInfo("[MobFan] Enabled: " + enabled);
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        MobFanComponent copy = new MobFanComponent();
        copy.fanLength = this.fanLength;
        copy.fanWidth = this.fanWidth;
        copy.fanHeight = this.fanHeight;
        copy.lengthUpgrades = this.lengthUpgrades;
        copy.widthUpgrades = this.widthUpgrades;
        copy.heightUpgrades = this.heightUpgrades;
        copy.enabled = this.enabled;
        copy.baseForward = this.baseForward;
        copy.noiseSuppressed = this.noiseSuppressed;
        copy.chunkLoaded = this.chunkLoaded;
        return copy;
    }
}
