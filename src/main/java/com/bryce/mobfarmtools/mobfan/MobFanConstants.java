package com.bryce.mobfarmtools.mobfan;

public final class MobFanConstants {
    public static final int FAN_UPGRADE_MAX = 4;
    public static final int FAN_LENGTH_MIN = 3;
    public static final int FAN_WIDTH_MIN = 3;
    public static final int FAN_HEIGHT_MIN = 3;
    public static final int FAN_LENGTH_MAX = FAN_LENGTH_MIN + FAN_UPGRADE_MAX;
    public static final int FAN_WIDTH_MAX = FAN_WIDTH_MIN + (FAN_UPGRADE_MAX * 2);
    public static final int FAN_HEIGHT_MAX = FAN_HEIGHT_MIN + (FAN_UPGRADE_MAX * 2);

    public static final String UPGRADE_LENGTH_ITEM_ID = "Mob_Fan_Upgrade_Length";
    public static final String UPGRADE_WIDTH_ITEM_ID = "Mob_Fan_Upgrade_Width";
    public static final String UPGRADE_HEIGHT_ITEM_ID = "Mob_Fan_Upgrade_Height";
}
