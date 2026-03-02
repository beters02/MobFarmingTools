package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.machineupgrade.ui.UpgradePageStatDef;
import com.hypixel.hytale.protocol.VelocityThresholdStyle;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;

public final class MobFanConstants {
    public static final int FAN_UPGRADE_MAX = 4;
    public static final int FAN_LENGTH_MIN = 3;
    public static final int FAN_WIDTH_MIN = 3;
    public static final int FAN_HEIGHT_MIN = 3;
    public static final int FAN_LENGTH_MAX = FAN_LENGTH_MIN + FAN_UPGRADE_MAX;
    public static final int FAN_WIDTH_MAX = FAN_WIDTH_MIN + (FAN_UPGRADE_MAX * 2);
    public static final int FAN_HEIGHT_MAX = FAN_HEIGHT_MIN + (FAN_UPGRADE_MAX * 2);

    public static final boolean DEF_ENABLED = true;
    public static final boolean DEF_CHUNK_LOADED = false;
    public static final boolean DEF_NOISE_SUPPRESSED = false;

    public static final float FAN_SPEED_VEL = 100f;
    public static final double FAN_SPEED_POS = 0.2; // speed for position related pushing

    public static final VelocityConfig FAN_VELOCITY_CONFIG = createFanVelocityConfig();

    private static VelocityConfig createFanVelocityConfig() {
        VelocityConfig cfg = new VelocityConfig();

        // Keep resistance stable at low speeds (prevents freeze/stall)
        setFloatField(cfg, "groundResistance", 0.96f);
        setFloatField(cfg, "groundResistanceMax", 0.96f);
        setFloatField(cfg, "airResistance", 0.5f);
        setFloatField(cfg, "airResistanceMax", 0.5f);

        // Threshold/style no longer critical when min==max, but set explicitly anyway
        setFloatField(cfg, "threshold", 0.05f);
        setField(cfg, "style", VelocityThresholdStyle.Linear);

        return cfg;
    }

    private static void setFloatField(VelocityConfig cfg, String name, float value) {
        try {
            java.lang.reflect.Field f = VelocityConfig.class.getDeclaredField(name);
            f.setAccessible(true);
            f.setFloat(cfg, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set VelocityConfig field: " + name, e);
        }
    }

    private static void setField(VelocityConfig cfg, String name, Object value) {
        try {
            java.lang.reflect.Field f = VelocityConfig.class.getDeclaredField(name);
            f.setAccessible(true);
            f.set(cfg, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to set VelocityConfig field: " + name, e);
        }
    }

    public enum UpgradePageStat implements UpgradePageStatDef {
        ENABLED(0, "Enabled", String.valueOf(DEF_ENABLED)),
        CHUNK_LOADED(1, "Chunk Loaded", String.valueOf(DEF_CHUNK_LOADED)),
        NOISE_SUPPRESSED(2, "Noise Suppressed", String.valueOf(DEF_NOISE_SUPPRESSED)),
        LENGTH(3, "Fan Length", String.valueOf(FAN_LENGTH_MIN)),
        WIDTH(4, "Fan Width", String.valueOf(FAN_WIDTH_MIN)),
        HEIGHT(5, "Fan Height", String.valueOf(FAN_HEIGHT_MIN));

        private final int index;
        private final String description;
        private final String defaultValue;

        UpgradePageStat(int index, String description, String defaultValue) {
            this.index = index;
            this.description = description;
            this.defaultValue = defaultValue;
        }

        public int getIndex() {
            return index;
        }

        public String getDescription() {
            return description;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }
}
