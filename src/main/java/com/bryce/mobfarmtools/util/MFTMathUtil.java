package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;

import java.util.List;

public class MFTMathUtil {

    public static void PrintList(List<?> list) {
        list.forEach(v -> MobFarmingToolsPlugin.LOGGER.atWarning().log(String.valueOf(v)));
    }

}
