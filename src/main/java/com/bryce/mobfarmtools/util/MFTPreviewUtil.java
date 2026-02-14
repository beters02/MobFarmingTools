package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.PlayerRef;

public class MFTPreviewUtil {

    private static final Vector3f DEFAULT_PREVIEW_COLOR = new Vector3f(0.1f, 1.0f, 0.9f);
    private static final float DEFAULT_PREVIEW_DURATION_SECONDS = 86400.0f;

    private static void ShowBoxPreview(PlayerRef playerRef, Matrix4d matrix, Vector3f previewColor, float previewDurationSeconds) {
        playerRef.getPacketHandler().write(new ClearDebugShapes());
        DisplayDebug packet = new DisplayDebug(DebugShape.Cube, matrix.asFloatData(), previewColor, previewDurationSeconds, true, null);
        playerRef.getPacketHandler().write(packet);
    }

    public static void ShowBoxPreview(
            PlayerRef playerRef,
            BlockPosition blockPosition,
            int rotationIndex,
            double length,
            double width,
            double height,
            Vector3d baseForward,
            Vector3f previewColor,
            float previewDurationSeconds
    ) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        Vector3d forward = Rotation.rotate(baseForward, rot.yaw(), rot.pitch(), rot.roll()).normalize();
        Vector3d blockCenter = new Vector3d(blockPosition.x + 0.5, blockPosition.y + 0.5, blockPosition.z + 0.5);

        double start = 0.5;
        Vector3d boxCenter = blockCenter.clone().add(forward.clone().scale(start + length * 0.5));

        Matrix4d matrix = new Matrix4d().identity();
        Matrix4d tmp = new Matrix4d();
        matrix.translate(boxCenter);
        matrix.rotateEuler(rot.pitch().getRadians(), rot.yaw().getRadians(), rot.roll().getRadians(), tmp);
        matrix.scale(width, height, length);

        ShowBoxPreview(playerRef, matrix, previewColor, previewDurationSeconds);
    }

    public static void ShowBoxPreviewFromMiddle(
            PlayerRef playerRef,
            BlockPosition blockPosition,
            int rotationIndex,
            double length,
            double width,
            double height
    ) {
        RotationTuple rot = RotationTuple.get(rotationIndex);
        Vector3d blockCenter = new Vector3d(blockPosition.x, blockPosition.y, blockPosition.z);

        Matrix4d matrix = new Matrix4d().identity();
        Matrix4d tmp = new Matrix4d();
        matrix.translate(blockCenter);
        matrix.rotateEuler(rot.pitch().getRadians(), rot.yaw().getRadians(), rot.roll().getRadians(), tmp);
        matrix.scale(width, height, length);

        ShowBoxPreview(playerRef, matrix, DEFAULT_PREVIEW_COLOR, DEFAULT_PREVIEW_DURATION_SECONDS);
    }

    public static void ShowBoxPreview(PlayerRef playerRef, Box box) {
        Matrix4d boxMatrix = new Matrix4d()
                .identity()
                .translate(box.middleX(), box.middleY(), box.middleZ())
                .scale(box.width(), box.height(), box.depth());

        ShowBoxPreview(playerRef, boxMatrix, DEFAULT_PREVIEW_COLOR, DEFAULT_PREVIEW_DURATION_SECONDS);
    }

    public static void ShowBoxPreview(
            PlayerRef playerRef,
            BlockPosition blockPosition,
            int rotationIndex,
            double length,
            double width,
            double height,
            Vector3d baseForward
    ) {
        ShowBoxPreview(playerRef, blockPosition, rotationIndex, length, width, height, baseForward, DEFAULT_PREVIEW_COLOR, DEFAULT_PREVIEW_DURATION_SECONDS);
    }

    public static void ShowBoxPreview(
            PlayerRef playerRef,
            BlockPosition blockPosition,
            int rotationIndex,
            double length,
            double width,
            double height
    ) {
        ShowBoxPreview(playerRef, blockPosition, rotationIndex, length, width, height, new Vector3d(), DEFAULT_PREVIEW_COLOR, DEFAULT_PREVIEW_DURATION_SECONDS);
    }

    public static void ShowBoxPreview(
            PlayerRef playerRef,
            BlockPosition blockPosition,
            int rotationIndex,
            double length,
            double width,
            double height,
            Vector3f previewColor,
            float previewDurationSeconds
    ) {
        ShowBoxPreview(playerRef, blockPosition, rotationIndex, length, width, height, new Vector3d(), previewColor, previewDurationSeconds);
    }

}
