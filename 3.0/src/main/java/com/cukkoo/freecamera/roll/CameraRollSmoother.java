package com.cukkoo.freecamera.roll;

public final class CameraRollSmoother {
    private static final double VELOCITY_EPSILON = 1.0E-5;

    public void integrate(
            CameraRollState state,
            double targetVelocity,
            double frequency,
            double elapsedSeconds
    ) {
        double initialVelocity = state.velocity();
        double decay = Math.exp(-frequency * elapsedSeconds);
        double velocityDifference = initialVelocity - targetVelocity;
        double angleDelta = targetVelocity * elapsedSeconds
                + velocityDifference * (1.0 - decay) / frequency;
        double nextVelocity = targetVelocity + velocityDifference * decay;
        if (Math.abs(nextVelocity) <= VELOCITY_EPSILON && targetVelocity == 0.0) {
            nextVelocity = 0.0;
        }
        state.integrate(angleDelta, nextVelocity);
    }
}
