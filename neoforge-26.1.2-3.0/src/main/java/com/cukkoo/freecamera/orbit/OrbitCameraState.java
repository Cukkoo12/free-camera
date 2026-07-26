package com.cukkoo.freecamera.orbit;

import net.minecraft.util.Mth;

public final class OrbitCameraState {
    public static final double MIN_RADIUS = 1.0;
    public static final double MAX_RADIUS = 64.0;
    public static final double DEFAULT_RADIUS = 4.0;
    public static final float MAX_VERTICAL_ANGLE = 85.0F;
    private static final double MAX_ANCHOR_OFFSET = 64.0;

    private double yaw;
    private double pitch;
    private double radius = DEFAULT_RADIUS;
    private double anchorOffsetY;
    private double requestedRadius = DEFAULT_RADIUS;
    private boolean radiusTargetActive;

    public float yaw() {
        return (float) yaw;
    }

    public float pitch() {
        return (float) pitch;
    }

    public double radius() {
        return radius;
    }

    public double anchorOffsetY() {
        return anchorOffsetY;
    }

    double requestedRadius() {
        return requestedRadius;
    }

    boolean hasRadiusTarget() {
        return radiusTargetActive;
    }

    public void initialize(float yaw, float pitch, double radius, double anchorOffsetY) {
        this.yaw = Mth.wrapDegrees((double) yaw);
        this.pitch = Math.clamp((double) pitch, -MAX_VERTICAL_ANGLE, MAX_VERTICAL_ANGLE);
        this.radius = clampRadius(radius);
        this.anchorOffsetY = Math.clamp(anchorOffsetY, -MAX_ANCHOR_OFFSET, MAX_ANCHOR_OFFSET);
        requestedRadius = this.radius;
        radiusTargetActive = false;
    }

    public void rotate(float yawDelta, float pitchDelta) {
        yaw = Mth.wrapDegrees(yaw + yawDelta);
        pitch = Math.clamp(pitch + pitchDelta, -MAX_VERTICAL_ANGLE, MAX_VERTICAL_ANGLE);
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = Mth.wrapDegrees((double) yaw);
        this.pitch = Math.clamp((double) pitch, -MAX_VERTICAL_ANGLE, MAX_VERTICAL_ANGLE);
    }

    void addYaw(double delta) {
        yaw = Mth.wrapDegrees(yaw + delta);
    }

    void addAnchorOffset(double delta) {
        anchorOffsetY = Math.clamp(anchorOffsetY + delta, -MAX_ANCHOR_OFFSET, MAX_ANCHOR_OFFSET);
    }

    boolean setIntegratedRadius(double value) {
        double clamped = clampRadius(value);
        boolean hitBoundary = clamped != value;
        radius = clamped;
        return hitBoundary;
    }

    public boolean requestRadiusStep(int scrollDirection) {
        if (scrollDirection == 0) {
            return false;
        }
        double base = radiusTargetActive ? requestedRadius : radius;
        double next = clampRadius(base - Integer.signum(scrollDirection));
        if (next == base) {
            return false;
        }
        requestedRadius = next;
        radiusTargetActive = true;
        return true;
    }

    void cancelRadiusTarget() {
        requestedRadius = radius;
        radiusTargetActive = false;
    }

    public void clear() {
        yaw = 0.0F;
        pitch = 0.0F;
        radius = DEFAULT_RADIUS;
        anchorOffsetY = 0.0;
        requestedRadius = DEFAULT_RADIUS;
        radiusTargetActive = false;
    }

    public static double clampRadius(double radius) {
        if (!Double.isFinite(radius)) {
            return DEFAULT_RADIUS;
        }
        return Math.clamp(radius, MIN_RADIUS, MAX_RADIUS);
    }
}
