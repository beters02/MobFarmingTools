package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.machineupgrade.ui.UpgradePageStatDef;

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

    public static final String UPGRADE_LENGTH_ITEM_ID = "Mob_Fan_Upgrade_Length";
    public static final String UPGRADE_WIDTH_ITEM_ID = "Mob_Fan_Upgrade_Width";
    public static final String UPGRADE_HEIGHT_ITEM_ID = "Mob_Fan_Upgrade_Height";

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
