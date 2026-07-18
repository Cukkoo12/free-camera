package com.cukkoo.freecamera.roll;

public final class CameraRollState {
    public static final double DEFAULT_RATE = 60.0;

    private double currentRoll;
    private double velocity;

    public double currentRoll() {
        return currentRoll;
    }

    public double velocity() {
        return velocity;
    }

    void integrate(double angleDelta, double velocity) {
        currentRoll = normalizeDegrees(currentRoll + angleDelta);
        this.velocity = Double.isFinite(velocity) ? velocity : 0.0;
    }

    void holdCurrent() {
        velocity = 0.0;
    }

    public void resetToZero() {
        currentRoll = 0.0;
        velocity = 0.0;
    }
    public void setPlaybackRoll(double roll) { currentRoll = normalizeDegrees(roll); velocity = 0.0; }

    public void clear() {
        resetToZero();
    }

    public static double normalizeDegrees(double angle) {
        if (!Double.isFinite(angle)) {
            return 0.0;
        }
        double normalized = angle % 360.0;
        if (normalized >= 180.0) {
            normalized -= 360.0;
        } else if (normalized < -180.0) {
            normalized += 360.0;
        }
        return normalized == 0.0 ? 0.0 : normalized;
    }
}
