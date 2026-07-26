package com.cukkoo.freecamera.zoom;

public final class CameraZoomState {
    public static final double NORMAL_MULTIPLIER = 1.0;
    public static final double DEFAULT_ZOOM_MULTIPLIER = 3.0;

    private double currentMultiplier = NORMAL_MULTIPLIER;
    private double targetMultiplier = NORMAL_MULTIPLIER;
    private double velocity;
    private double configuredMultiplier=DEFAULT_ZOOM_MULTIPLIER;
    public void configure(double multiplier){configuredMultiplier=Double.isFinite(multiplier)?Math.clamp(multiplier,1,10):DEFAULT_ZOOM_MULTIPLIER;}
    public double configuredMultiplier(){return configuredMultiplier;}

    public double currentMultiplier() { return currentMultiplier; }
    public double targetMultiplier() { return targetMultiplier; }
    public double velocity() { return velocity; }

    void setTargetMultiplier(double targetMultiplier) {
        this.targetMultiplier = Math.clamp(
                targetMultiplier, NORMAL_MULTIPLIER, configuredMultiplier);
    }

    void setIntegrated(double currentMultiplier, double velocity) {
        this.currentMultiplier = Math.clamp(
                currentMultiplier, NORMAL_MULTIPLIER, configuredMultiplier);
        this.velocity = Double.isFinite(velocity) ? velocity : 0.0;
    }

    void settleAtTarget() {
        currentMultiplier = targetMultiplier;
        velocity = 0.0;
    }

    void freezeAtCurrent() {
        targetMultiplier = currentMultiplier;
        velocity = 0.0;
    }

    public void clear() {
        currentMultiplier = NORMAL_MULTIPLIER;
        targetMultiplier = NORMAL_MULTIPLIER;
        velocity = 0.0;
    }
    public void setPlaybackMultiplier(double value) { currentMultiplier=Math.clamp(value,1.0,10.0);targetMultiplier=currentMultiplier;velocity=0; }
}
