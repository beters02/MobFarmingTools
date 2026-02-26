package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTMathUtil;
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

    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobFanComponent]", false);

    private int fanLength = 3;
    private int fanWidth = 3;
    private int fanHeight = 3;
    private int lengthUpgrades = 0;
    private int widthUpgrades = 0;
    private int heightUpgrades = 0;
    private boolean enabled = true;
    private boolean noiseSuppressed = false;
    private boolean chunkLoaded = false;

    private World _world;
    private Vector3i _worldPos;
    private Vector3d baseForward = new Vector3d(0, 0, -1);

    @NonNull
    public static ComponentType<ChunkStore, MobFanComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMobFanComponentType();
    }

    public void setFanLength(int amount) {
        if (amount < MobFanConstants.FAN_LENGTH_MIN || amount > MobFanConstants.FAN_LENGTH_MAX) return;
        fanLength = amount;
    }

    public void setFanWidth(int amount) {
        if (amount < MobFanConstants.FAN_WIDTH_MIN || amount > MobFanConstants.FAN_WIDTH_MAX) return;
        fanWidth = amount;
    }

    public void setFanHeight(int amount) {
        if (amount < MobFanConstants.FAN_HEIGHT_MIN || amount > MobFanConstants.FAN_HEIGHT_MAX) return;
        fanHeight = amount;
    }

    public void setEnabled(boolean enabled) {
        if (this._worldPos == null) return;
        if (this._world == null) return;

        BlockType blockType = this._world.getBlockType(this._worldPos);
        if (blockType == null) return;

        this.enabled = enabled;
        this._world.setBlockInteractionState(this._worldPos, blockType, enabled ? "On" : "Off");
    }

    // GETTERS
    public boolean isEnabled() {
        return this.enabled;
    }
    public boolean isNoiseSuppressed() {
        return noiseSuppressed;
    }
    public boolean isChunkLoaded() {
        return chunkLoaded;
    }
    public int getFanLength() { return fanLength; }
    public int getFanWidth() {
        return fanWidth;
    }
    public int getFanHeight() {
        return fanHeight;
    }
    public int getLengthUpgrades() {
        return lengthUpgrades;
    }
    public int getWidthUpgrades() {
        return widthUpgrades;
    }
    public int getHeightUpgrades() {
        return heightUpgrades;
    }
    public Vector3d getBaseForward() {
        return this.baseForward;
    }
    public MFTMathUtil.Volume3i getFanSize() {
        return new MFTMathUtil.Volume3i(fanLength, fanWidth, fanHeight);
    }

    // SETTERS
    public void setStoredWorld(World world) {
        this._world = world;
    }
    public void setStoredWorldPos(Vector3i pos) {
        this._worldPos = pos;
    }
    public void setBaseForward(Vector3d forward) {
        this.baseForward = forward;
    }
    public void setNoiseSuppressed(boolean value) {
        noiseSuppressed = value;
    }
    public void setChunkLoaded(boolean value) {
        chunkLoaded = value;
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

    // HELPERS
    private void applyUpgradeCounts() {
        lengthUpgrades = clampUpgrade(lengthUpgrades);
        widthUpgrades = clampUpgrade(widthUpgrades);
        heightUpgrades = clampUpgrade(heightUpgrades);
        setFanLength(MobFanConstants.FAN_LENGTH_MIN + lengthUpgrades);
        setFanWidth(MobFanConstants.FAN_WIDTH_MIN + (widthUpgrades * 2));
        setFanHeight(MobFanConstants.FAN_HEIGHT_MIN + (heightUpgrades * 2));
    }

    private static int clampUpgrade(int amount) {
        return Math.clamp(amount, 0, MobFanConstants.FAN_UPGRADE_MAX);
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
