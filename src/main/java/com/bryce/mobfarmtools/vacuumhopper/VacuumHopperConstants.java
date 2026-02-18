package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.machineupgrade.ui.UpgradePageStatDef;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;

public class VacuumHopperConstants {
    public static int TICKS_PER_ACTION = 10;

    public static double ITEM_PICKUP_RADIUS = 2;
    public static double ITEM_SUCK_RADIUS = 3;

    public static double ITEM_SUCK_SPEED = 150;
    public static double ITEM_SUCK_Y = 3;

    public static boolean DEBUGGER_ENABLED = false;

    public static boolean DEF_NOISE_SUPPRESSED = false;
    public static boolean DEF_ENABLED = true;
    public static boolean DEF_CHUNK_LOADED = false;

    public static int SIZE_UPGRADE_MAX = 4;

    public static int LENGTH_MIN = 3;
    public static int WIDTH_MIN = 3;
    public static int HEIGHT_MIN = 3;

    public static final int LENGTH_MAX = LENGTH_MIN + (SIZE_UPGRADE_MAX * 2);
    public static final int WIDTH_MAX = WIDTH_MIN + (SIZE_UPGRADE_MAX * 2);
    public static final int HEIGHT_MAX = HEIGHT_MIN + (SIZE_UPGRADE_MAX * 2);

    public enum UpgradePageStat implements UpgradePageStatDef {
        ENABLED(0, "Enabled", String.valueOf(DEF_ENABLED)),
        CHUNK_LOADED(1, "Chunk Loaded", String.valueOf(DEF_CHUNK_LOADED)),
        NOISE_SUPPRESSED(2, "Noise Suppressed", String.valueOf(DEF_NOISE_SUPPRESSED)),
        LENGTH(3, "Vacuum Length", String.valueOf(ITEM_SUCK_RADIUS)),
        WIDTH(4, "Vacuum Width", String.valueOf(ITEM_SUCK_RADIUS)),
        HEIGHT(5, "Vacuum Height", String.valueOf(ITEM_SUCK_RADIUS));

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
