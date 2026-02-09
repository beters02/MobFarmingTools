package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.math.vector.Vector3d;

public class MFTVectorUtil {

    public static Vector3d multiply(Vector3d v1, Vector3d v2) {
        v1.x *= v2.x;
        v1.y *= v2.y;
        v1.z *= v2.z;
        return v1;
    }

    public static Vector3d multiply(Vector3d v1, double v2) {
        v1.x *= v2;
        v1.y *= v2;
        v1.z *= v2;
        return v1;
    }

}