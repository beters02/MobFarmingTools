package com.bryce.mobfarmtools.machineupgrade;

import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import org.jspecify.annotations.Nullable;

public enum MachineUpgradeType {
    WIDTH(0, "Width", MobFanConstants.UPGRADE_WIDTH_ITEM_ID),
    HEIGHT(1, "Height", MobFanConstants.UPGRADE_HEIGHT_ITEM_ID),
    LENGTH(2, "Length", MobFanConstants.UPGRADE_LENGTH_ITEM_ID),
    NOISE_SUPPRESSION(3, "Noise Suppression", "Machine_Upgrade_Noise_Suppression"),
    SPEED(4, "Speed", "Machine_Upgrade_Speed"),
    OUTPUT(5, "Output", "Machine_Upgrade_Output"),
    CHUNK_LOADING(6, "Chunk Loading", "Machine_Upgrade_Chunk_Loading");

    private final int index;
    private final String displayName;
    private final String itemId;

    MachineUpgradeType(int index, String displayName, String itemId) {
        this.index = index;
        this.displayName = displayName;
        this.itemId = itemId;
    }

    public int getIndex() {
        return index;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getItemId() {
        return itemId;
    }

    @Nullable
    public static MachineUpgradeType fromIndex(int index) {
        for (MachineUpgradeType type : values()) {
            if (type.index == index) {
                return type;
            }
        }
        return null;
    }

    @Nullable
    public static MachineUpgradeType fromItemId(String itemId) {
        for (MachineUpgradeType type : values()) {
            if (type.itemId.equals(itemId)) {
                return type;
            }
        }
        return null;
    }
}

