package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VacuumHopperComponent implements Component<ChunkStore> {
    public static final BuilderCodec<VacuumHopperComponent> CODEC =
            BuilderCodec.builder(VacuumHopperComponent.class, VacuumHopperComponent::new)
                    .afterDecode(VacuumHopperComponent::applyUpgradeCounts)
                    .build();

    private boolean enabled = VacuumHopperConstants.DEF_ENABLED;
    private boolean noiseSuppressed = VacuumHopperConstants.DEF_NOISE_SUPPRESSED;
    private boolean chunkLoaded = VacuumHopperConstants.DEF_CHUNK_LOADED;
    private int length = (int) VacuumHopperConstants.ITEM_SUCK_RADIUS;
    private int width = (int) VacuumHopperConstants.ITEM_SUCK_RADIUS;
    private int height = (int) VacuumHopperConstants.ITEM_SUCK_RADIUS;

    private int lengthUpgrades = 0;
    private int widthUpgrades = 0;
    private int heightUpgrades = 0;

    private float lifetime = 0f;
    private int ticksLifetime = 0;
    private boolean hasAvailableContainer = false;

    public boolean isEnabled() {
        return enabled;
    }
    public boolean isNoiseSuppressed() { return noiseSuppressed; }
    public boolean isChunkLoaded() { return chunkLoaded; }
    public boolean hasAvailableContainer() { return hasAvailableContainer; }
    public float getLifetime() {
        return lifetime;
    }
    public float getTicksLifetime() {
        return ticksLifetime;
    }
    public int getLength() { return length; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public int getLengthUpgrades() { return lengthUpgrades; }
    public int getWidthUpgrades() { return widthUpgrades; }
    public int getHeightUpgrades() { return heightUpgrades; }

    public void setLifetime(float value) {
        lifetime = value;
    }
    public void setTicksLifetime(int value) {
        ticksLifetime = value;
    }
    public void setEnabled(boolean value) {
        enabled = value;
    }
    public void setHasAvailableContainer(boolean value) { hasAvailableContainer = value; }
    private void setLength(int value) { length = value; }
    private void setWidth(int value) { width = value; }
    private void setHeight(int value) { height = value; }
    public void setNoiseSuppressed(boolean value) { noiseSuppressed = value; }
    public void setChunkLoaded(boolean value) { chunkLoaded = value; }
    public void setLengthUpgrades(int value) {
        lengthUpgrades = value;
        applyUpgradeCounts();
    }
    public void setWidthUpgrades(int value) {
        widthUpgrades = value;
        applyUpgradeCounts();
    }
    public void setHeightUpgrades(int value) {
        heightUpgrades = value;
        applyUpgradeCounts();
    }

    public void incrementLifetime(float amount) {
        lifetime += amount;
    }
    public void incrementTicksLifetime(int amount) {
        ticksLifetime += amount;
    }

    private void applyUpgradeCounts() {
        lengthUpgrades = clampUpgrade(lengthUpgrades);
        widthUpgrades = clampUpgrade(widthUpgrades);
        heightUpgrades = clampUpgrade(heightUpgrades);
        setLength(VacuumHopperConstants.LENGTH_MIN + (lengthUpgrades * 2));
        setWidth(VacuumHopperConstants.WIDTH_MIN + (widthUpgrades * 2));
        setHeight(VacuumHopperConstants.HEIGHT_MIN + (heightUpgrades * 2));
    }

    private static int clampUpgrade(int amount) {
        if (amount < 0) {
            return 0;
        }
        return Math.min(amount, VacuumHopperConstants.SIZE_UPGRADE_MAX);
    }

    @NonNull
    public static ComponentType<ChunkStore, VacuumHopperComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getVacuumHopperComponentType();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        VacuumHopperComponent copy = new VacuumHopperComponent();
        copy.lifetime = lifetime;
        copy.ticksLifetime = ticksLifetime;
        copy.hasAvailableContainer = hasAvailableContainer;

        copy.enabled = enabled;
        copy.noiseSuppressed = noiseSuppressed;
        copy.chunkLoaded = chunkLoaded;
        copy.length = length;
        copy.width = width;
        copy.height = height;

        copy.lengthUpgrades = lengthUpgrades;
        copy.widthUpgrades = widthUpgrades;
        copy.heightUpgrades = heightUpgrades;

        copy.applyUpgradeCounts();
        return copy;
    }
}