package com.cukkoo.freecamera.collision;

public final class CameraCollisionSmoother {
    private static final double RECOVERY_FREQUENCY = 8.0;

    public double recoveryFraction(double elapsedSeconds) {
        if (!Double.isFinite(elapsedSeconds) || elapsedSeconds <= 0.0) {
            return 0.0;
        }
        return 1.0 - Math.exp(-RECOVERY_FREQUENCY * elapsedSeconds);
    }
}
