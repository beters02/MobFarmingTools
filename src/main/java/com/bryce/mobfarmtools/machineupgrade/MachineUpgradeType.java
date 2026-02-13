package com.bryce.mobfarmtools.machineupgrade;

import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import org.jspecify.annotations.Nullable;

public enum MachineUpgradeType {
    WIDTH(0, "Width", MachineUpgradeConstants.UPGRADE_WIDTH_ITEM_ID),
    HEIGHT(1, "Height", MachineUpgradeConstants.UPGRADE_HEIGHT_ITEM_ID),
    LENGTH(2, "Length", MachineUpgradeConstants.UPGRADE_LENGTH_ITEM_ID),
    NOISE_SUPPRESSION(3, "Noise Suppression", MachineUpgradeConstants.UPGRADE_NOISE_SUPPRESSION_ITEM_ID),
    SPEED(4, "Speed", MachineUpgradeConstants.UPGRADE_SPEED_ITEM_ID),
    OUTPUT(5, "Output", MachineUpgradeConstants.UPGRADE_OUTPUT_ITEM_ID),
    CHUNK_LOADING(6, "Chunk Loading", MachineUpgradeConstants.UPGRADE_CHUNK_LOADING_ITEM_ID);

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

