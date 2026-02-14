package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeConstants;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.UpgradePageStatDef;
import org.jspecify.annotations.Nullable;

public class MobSpawnerConstants {

    public static final int MAX_FAILED_SPAWN_TRIES = 5;
    public static final double SPAWN_RADIUS = 3; // 7x7

    public static final int DEF_SPAWN_RATE_MIN = 30;
    public static final int DEF_SPAWN_RATE_MAX = 60;
    public static final int DEF_SPAWN_AMOUNT_MIN = 1;
    public static final int DEF_SPAWN_AMOUNT_MAX = 1;
    public static final int DEF_MAX_ENTITIES = 5;
    public static final boolean DEF_CHUNK_LOADED = false;
    public static final boolean DEF_ENABLED = true;
    public static final boolean DEF_NOISE_SUPPRESSED = false;
    public static final String DEF_ENTITY_ID = "None";

    public static final int UPG1_SPAWN_RATE_MIN = 20;
    public static final int UPG1_SPAWN_RATE_MAX = 40;
    public static final int UPG1_SPAWN_AMOUNT_MIN = 1;
    public static final int UPG1_SPAWN_AMOUNT_MAX = 2;

    public static final int UPG2_SPAWN_RATE_MIN = 20;
    public static final int UPG2_SPAWN_RATE_MAX = 30;
    public static final int UPG2_SPAWN_AMOUNT_MIN = 2;
    public static final int UPG2_SPAWN_AMOUNT_MAX = 2;

    public static final int UPG3_SPAWN_RATE_MIN = 15;
    public static final int UPG3_SPAWN_RATE_MAX = 20;

    public static final int UPG4_SPAWN_RATE_MIN = 10;
    public static final int UPG4_SPAWN_RATE_MAX = 15;

    public static final int TICKS_UPDATE_STAT = 15;

    public enum UpgradePageStat implements UpgradePageStatDef {
        ENTITY_ID(0, "Stored Entity", DEF_ENTITY_ID),
        ENABLED(1, "Enabled", String.valueOf(DEF_ENABLED)),
        CHUNK_LOADED(2, "Chunk Loaded", String.valueOf(DEF_CHUNK_LOADED)),
        NOISE_SUPPRESSED(3, "Noise Suppressed", String.valueOf(DEF_NOISE_SUPPRESSED)),
        SPAWN_RATE(4, "Spawn Rate Min, Max", MobSpawnerComponent.getStatValue(DEF_SPAWN_RATE_MIN, DEF_SPAWN_RATE_MAX)),
        SPAWN_AMOUNT(5, "Spawn Amount Min, Max", MobSpawnerComponent.getStatValue(DEF_SPAWN_AMOUNT_MIN, DEF_SPAWN_AMOUNT_MAX)),
        NEXT_SPAWN_TIME(6, "Time Until Next Spawn", "-1");

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
