package com.cukkoo.freecamera.collision;

import com.cukkoo.freecamera.api.CameraMode;

public final class CameraCollisionState {
    private CameraCollisionMode freeMode = CameraCollisionMode.OFF;
    private CameraCollisionMode orbitMode = CameraCollisionMode.SOFT;
    private CameraCollisionMode followMode = CameraCollisionMode.SOFT;
    private CameraMode activeCameraMode;
    private boolean initialized;
    private boolean obstructed;
    private boolean hasSafePosition;
    private double resolvedX;
    private double resolvedY;
    private double resolvedZ;
    private double safeX;
    private double safeY;
    private double safeZ;
    private boolean hasPreviousSafePosition;
    private double previousSafeX;
    private double previousSafeY;
    private double previousSafeZ;
    private double lastDesiredX;
    private double lastDesiredY;
    private double lastDesiredZ;
    private double lastAnchorX;
    private double lastAnchorY;
    private double lastAnchorZ;
    private long lastFrameNanos;

    public CameraCollisionMode modeFor(CameraMode mode) {
        return switch (mode) {
            case FREE_CAMERA -> freeMode;
            case ORBIT -> orbitMode;
            case TRIPOD -> CameraCollisionMode.OFF;
            case FOLLOW -> followMode;
        };
    }

    public void setModeFor(CameraMode mode, CameraCollisionMode collisionMode) {
        if (mode == CameraMode.FREE_CAMERA) {
            freeMode = collisionMode;
        } else if (mode == CameraMode.ORBIT) {
            orbitMode = collisionMode;
        } else if (mode == CameraMode.FOLLOW) {
            followMode = collisionMode;
        }
    }

    boolean isInitializedFor(CameraMode mode) {
        return initialized && activeCameraMode == mode;
    }

    void seed(CameraMode mode, double x, double y, double z) {
        activeCameraMode = mode;
        initialized = true;
        obstructed = false;
        resolvedX = x;
        resolvedY = y;
        resolvedZ = z;
        lastDesiredX = x;
        lastDesiredY = y;
        lastDesiredZ = z;
        lastAnchorX = Double.NaN;
        lastAnchorY = Double.NaN;
        lastAnchorZ = Double.NaN;
        lastFrameNanos = 0L;
    }

    void updateResolved(double x, double y, double z, boolean obstructed) {
        resolvedX = x;
        resolvedY = y;
        resolvedZ = z;
        this.obstructed = obstructed;
    }

    void rememberSafe(double x, double y, double z) {
        if (hasSafePosition && (!same(safeX, x) || !same(safeY, y) || !same(safeZ, z))) {
            hasPreviousSafePosition = true;
            previousSafeX = safeX;
            previousSafeY = safeY;
            previousSafeZ = safeZ;
        }
        hasSafePosition = true;
        safeX = x;
        safeY = y;
        safeZ = z;
    }

    void rememberQuery(
            double desiredX, double desiredY, double desiredZ,
            double anchorX, double anchorY, double anchorZ
    ) {
        lastDesiredX = desiredX;
        lastDesiredY = desiredY;
        lastDesiredZ = desiredZ;
        lastAnchorX = anchorX;
        lastAnchorY = anchorY;
        lastAnchorZ = anchorZ;
    }

    boolean desiredAndAnchorUnchanged(
            double desiredX, double desiredY, double desiredZ,
            double anchorX, double anchorY, double anchorZ
    ) {
        return same(lastDesiredX, desiredX) && same(lastDesiredY, desiredY)
                && same(lastDesiredZ, desiredZ) && same(lastAnchorX, anchorX)
                && same(lastAnchorY, anchorY) && same(lastAnchorZ, anchorZ);
    }

    double elapsedSeconds(long nowNanos) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = nowNanos;
            return 0.0;
        }
        double elapsed = (nowNanos - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = nowNanos;
        return Double.isFinite(elapsed) && elapsed > 0.0 && elapsed <= 0.25 ? elapsed : 0.0;
    }

    public void suspend() {
        lastFrameNanos = 0L;
    }

    public void clear() {
        activeCameraMode = null;
        initialized = false;
        obstructed = false;
        hasSafePosition = false;
        hasPreviousSafePosition = false;
        resolvedX = resolvedY = resolvedZ = 0.0;
        safeX = safeY = safeZ = 0.0;
        previousSafeX = previousSafeY = previousSafeZ = 0.0;
        lastDesiredX = lastDesiredY = lastDesiredZ = 0.0;
        lastAnchorX = lastAnchorY = lastAnchorZ = Double.NaN;
        lastFrameNanos = 0L;
    }

    public boolean isObstructed() { return obstructed; }
    public boolean hasSafePosition() { return hasSafePosition; }
    public double resolvedX() { return resolvedX; }
    public double resolvedY() { return resolvedY; }
    public double resolvedZ() { return resolvedZ; }
    double safeX() { return safeX; }
    double safeY() { return safeY; }
    double safeZ() { return safeZ; }
    boolean hasPreviousSafePosition() { return hasPreviousSafePosition; }
    double previousSafeX() { return previousSafeX; }
    double previousSafeY() { return previousSafeY; }
    double previousSafeZ() { return previousSafeZ; }

    private static boolean same(double first, double second) {
        return Double.doubleToLongBits(first) == Double.doubleToLongBits(second);
    }
}
