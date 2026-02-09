package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MFTMathUtil {



    public static int RandomRange(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max+1);
    }

    public static Box GetBoxFromPosition(Vector3d pos, double radius) {
        return new Box(
                new Vector3d(pos.x - radius, pos.y-12, pos.z - radius),
                new Vector3d(pos.x + radius, pos.y+12, pos.z + radius)
        );
    }

}
