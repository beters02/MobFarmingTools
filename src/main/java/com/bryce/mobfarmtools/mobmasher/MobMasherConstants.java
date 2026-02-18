package com.bryce.mobfarmtools.mobmasher;

import com.bryce.mobfarmtools.machineupgrade.ui.UpgradePageStatDef;

public class MobMasherConstants {

    public static final boolean DEF_ENABLED = true;
    public static final boolean DEF_CHUNK_LOADED = false;
    public static final boolean DEF_NOISE_SUPPRESSED = false;

    public static final int OUTPUT_UPGRADE_MAX = 4;
    public static final int SPEED_UPGRADE_MAX = 2;

    public static final int DEF_DAMAGE = 11;
    public static final int DEF_TICKS_PER_ACTION = 30;

    public static final int UPG1_DAMAGE = 15;
    public static final int UPG2_DAMAGE = 22;
    public static final int UPG3_DAMAGE = 26;
    public static final int UPG4_DAMAGE = 31;

    public static final int UPG1_SPEED = 23;
    public static final int UPG2_SPEED = 15;

    public enum UpgradePageStat implements UpgradePageStatDef {
        ENABLED(0, "Enabled", String.valueOf(DEF_ENABLED)),
        CHUNK_LOADED(1, "Chunk Loaded", String.valueOf(DEF_CHUNK_LOADED)),
        NOISE_SUPPRESSED(2, "Noise Suppressed", String.valueOf(DEF_NOISE_SUPPRESSED)),
        SPEED(3, "Masher Speed", String.valueOf(DEF_TICKS_PER_ACTION/30)),
        OUTPUT(4, "Masher Damage", String.valueOf(DEF_DAMAGE));

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
