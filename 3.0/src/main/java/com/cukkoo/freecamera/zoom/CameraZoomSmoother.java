package com.cukkoo.freecamera.zoom;

public final class CameraZoomSmoother {
    private static final double POSITION_EPSILON = 1.0E-6;
    private static final double VELOCITY_EPSILON = 1.0E-6;

    public void integrate(CameraZoomState state, double frequency, double elapsedSeconds) {
        double error = state.currentMultiplier() - state.targetMultiplier();
        double velocity = state.velocity();
        double coefficient = velocity + frequency * error;
        double decay = Math.exp(-frequency * elapsedSeconds);
        double nextError = (error + coefficient * elapsedSeconds) * decay;
        double nextVelocity = (velocity - coefficient * frequency * elapsedSeconds) * decay;
        state.setIntegrated(state.targetMultiplier() + nextError, nextVelocity);
        if (Math.abs(state.currentMultiplier() - state.targetMultiplier()) <= POSITION_EPSILON
                && Math.abs(state.velocity()) <= VELOCITY_EPSILON) {
            state.settleAtTarget();
        }
    }
}
