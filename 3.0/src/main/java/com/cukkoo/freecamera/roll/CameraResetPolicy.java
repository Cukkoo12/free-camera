package com.cukkoo.freecamera.roll;

public final class CameraResetPolicy {
    public static final double ROLL_EPSILON = 1.0E-4;

    private CameraResetPolicy() {
    }

    public static boolean shouldResetRoll(double roll, double angularVelocity) {
        return Math.abs(roll) > ROLL_EPSILON
                || Math.abs(angularVelocity) > ROLL_EPSILON;
    }
}
