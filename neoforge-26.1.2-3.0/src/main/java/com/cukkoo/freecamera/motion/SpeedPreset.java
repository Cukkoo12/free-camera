package com.cukkoo.freecamera.motion;

public enum SpeedPreset {
    PRECISE(0.25, "speed.free-camera.precise"),
    SLOW(1.0, "speed.free-camera.slow"),
    NORMAL(4.0, "speed.free-camera.normal"),
    FAST(12.0, "speed.free-camera.fast"),
    FLYBY(32.0, "speed.free-camera.flyby");

    private static final SpeedPreset[] VALUES = values();

    private final double blocksPerSecond;
    private final String translationKey;

    SpeedPreset(double blocksPerSecond, String translationKey) {
        this.blocksPerSecond = blocksPerSecond;
        this.translationKey = translationKey;
    }

    public double blocksPerSecond() {
        return blocksPerSecond;
    }

    public String translationKey() {
        return translationKey;
    }

    public SpeedPreset step(int direction) {
        int nextIndex = Math.clamp(ordinal() + Integer.signum(direction), 0, VALUES.length - 1);
        return VALUES[nextIndex];
    }
}
