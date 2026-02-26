package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class MFTMathUtil {

    public static int RandomRange(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max+1);
    }

    public static Vector3d GetForwardDirection(Vector3d baseForward, int rotationIndex) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        return Rotation.rotate(baseForward, rot.yaw(), rot.pitch(), rot.roll()).normalize();
    }

    public static Box GetBoxFromPosition(Vector3d pos, double radius) {
        return new Box(
                new Vector3d(pos.x - radius, pos.y-12, pos.z - radius),
                new Vector3d(pos.x + radius, pos.y+12, pos.z + radius)
        );
    }

    // returns a box centered around boxCenter
    public static Box GetBoxFromPosition(
            Vector3d boxCenter,
            double length, double width, double height,
            Vector3d baseForward, int rotationIndex
    ) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        Vector3d forward = GetForwardDirection(baseForward, rotationIndex);
        Vector3d right = Rotation.rotate(new Vector3d(1, 0, 0), rot.yaw(), rot.pitch(), rot.roll()).normalize();
        Vector3d up = Rotation.rotate(new Vector3d(0, 1, 0), rot.yaw(), rot.pitch(), rot.roll()).normalize();

        Vector3d halfForward = forward.clone().scale(length * 0.5);
        Vector3d halfRight = right.clone().scale(width * 0.5);
        Vector3d halfUp = up.clone().scale(height * 0.5);

        Vector3d min = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        int[] signs = new int[]{-1, 1};
        for (int sx : signs) {
            for (int sy : signs) {
                for (int sz : signs) {
                    Vector3d corner = boxCenter.clone()
                            .add(halfRight.clone().scale(sx))
                            .add(halfUp.clone().scale(sy))
                            .add(halfForward.clone().scale(sz));
                    min.x = Math.min(min.x, corner.x);
                    min.y = Math.min(min.y, corner.y);
                    min.z = Math.min(min.z, corner.z);
                    max.x = Math.max(max.x, corner.x);
                    max.y = Math.max(max.y, corner.y);
                    max.z = Math.max(max.z, corner.z);
                }
            }
        }

        return new Box(min, max);
    }

    public static Box GetBoxInFrontOf(
            Vector3d blockCenter,
            double length, double width, double height,
            Vector3d baseForward, int rotationIndex
    ) {
        Vector3d forward = GetForwardDirection(baseForward, rotationIndex);
        Vector3d boxCenter = blockCenter.clone().add(forward.clone().scale(0.5 + length * 0.5));
        return GetBoxFromPosition(boxCenter, length, width, height, baseForward, rotationIndex);
    }

    public static Box GetBoxInFrontOf(Vector3d blockCenter, Volume3i size, Vector3d baseForward, int rotationIndex) {
        return GetBoxInFrontOf(blockCenter, size.l, size.w, size.h, baseForward, rotationIndex);
    }

    public static final class Volume3i {
        int l = 0;
        int w = 0;
        int h = 0;

        public Volume3i() {}

        public Volume3i(int l, int w, int h) {
            this.l = l;
            this.w = w;
            this.h = h;
        }
    }
}
