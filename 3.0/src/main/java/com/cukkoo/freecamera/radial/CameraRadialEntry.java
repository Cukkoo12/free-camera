package com.cukkoo.freecamera.radial;

import com.cukkoo.freecamera.api.CameraMode;
import net.minecraft.network.chat.Component;

public enum CameraRadialEntry {
    FREE(CameraMode.FREE_CAMERA, "radial.free-camera.free", 0.0, -1.0),
    ORBIT(CameraMode.ORBIT, "radial.free-camera.orbit", 1.0, 0.0),
    TRIPOD(CameraMode.TRIPOD, "radial.free-camera.tripod", 0.0, 1.0),
    FOLLOW(CameraMode.FOLLOW, "radial.free-camera.follow", -1.0, 0.0);

    private static final CameraRadialEntry[] VALUES = values();

    private final CameraMode mode;
    private final Component label;
    private final double directionX;
    private final double directionY;

    CameraRadialEntry(CameraMode mode, String translationKey, double directionX, double directionY) {
        this.mode = mode;
        this.label = Component.translatable(translationKey);
        this.directionX = directionX;
        this.directionY = directionY;
    }

    public CameraMode mode() { return mode; }
    public Component label() { return label; }
    public double directionX() { return directionX; }
    public double directionY() { return directionY; }

    public static CameraRadialEntry at(int index) {
        return index < 0 || index >= VALUES.length ? null : VALUES[index];
    }

    public static int count() {
        return VALUES.length;
    }

    public static int indexForVector(double x, double y) {
        int bestIndex = 0;
        double bestDot = Double.NEGATIVE_INFINITY;
        for (int index = 0; index < VALUES.length; index++) {
            CameraRadialEntry entry = VALUES[index];
            double dot = x * entry.directionX + y * entry.directionY;
            if (dot > bestDot) {
                bestDot = dot;
                bestIndex = index;
            }
        }
        return bestIndex;
    }
}
