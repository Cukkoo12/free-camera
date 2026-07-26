package com.cukkoo.freecamera.orbit;

import com.cukkoo.freecamera.input.CameraInputSnapshot;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import net.minecraft.client.Minecraft;

public final class OrbitMotionIntegrator {
    static final double MAX_SUBSTEP_SECONDS = 0.05;
    static final double MAX_ACCEPTED_ELAPSED_SECONDS = 0.25;
    static final int MAX_SUBSTEP_COUNT = 5;
    private static final double ANGULAR_SPEED = 90.0;
    private static final double RADIUS_SPEED = 8.0;
    private static final double ANCHOR_SPEED = 4.0;
    private static final double RADIUS_TARGET_EPSILON = 1.0E-4;

    private double yawVelocity;
    private double radiusVelocity;
    private double anchorVelocity;
    private long lastFrameNanos;

    public void advance(
            Minecraft client,
            OrbitCameraState state,
            CameraInputSnapshot input,
            CinematicMotionProfile profile
    ) {
        if (client.screen != null || !client.isWindowActive()) {
            clearTransientState(state);
            return;
        }
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        double elapsedSeconds = (now - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = now;
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearTransientState(state);
            return;
        }
        integrateAcceptedElapsed(state, input, profile, elapsedSeconds);
    }

    void advanceForDevelopmentCheck(
            OrbitCameraState state,
            CameraInputSnapshot input,
            double elapsedSeconds
    ) {
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearTransientState(state);
            return;
        }
        integrateAcceptedElapsed(state, input, CinematicMotionProfile.RESPONSIVE, elapsedSeconds);
    }

    public void advanceForDevelopmentCheck(
            OrbitCameraState state,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            double elapsedSeconds
    ) {
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearTransientState(state);
            return;
        }
        integrateAcceptedElapsed(state, input, profile, elapsedSeconds);
    }

    public void clearTransientState(OrbitCameraState state) {
        yawVelocity = 0.0;
        radiusVelocity = 0.0;
        anchorVelocity = 0.0;
        lastFrameNanos = 0L;
        state.cancelRadiusTarget();
    }

    double yawVelocityForDevelopmentCheck() {
        return yawVelocity;
    }

    double radiusVelocityForDevelopmentCheck() {
        return radiusVelocity;
    }

    double anchorVelocityForDevelopmentCheck() {
        return anchorVelocity;
    }

    private void integrateAcceptedElapsed(
            OrbitCameraState state,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            double elapsedSeconds
    ) {
        int substepCount = Math.min(
                MAX_SUBSTEP_COUNT,
                Math.max(1, (int) Math.ceil(elapsedSeconds / MAX_SUBSTEP_SECONDS))
        );
        double remainingSeconds = elapsedSeconds;
        for (int substep = 0; substep < substepCount; substep++) {
            double stepSeconds = Math.min(MAX_SUBSTEP_SECONDS, remainingSeconds);
            integrateSubstep(state, input, profile, stepSeconds);
            remainingSeconds -= stepSeconds;
        }
    }

    private void integrateSubstep(
            OrbitCameraState state,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            double stepSeconds
    ) {
        double yawTarget = -input.strafe() * ANGULAR_SPEED;
        double anchorTarget = input.vertical() * ANCHOR_SPEED;
        double yawResponse = responseFor(yawVelocity, yawTarget, profile);
        double anchorResponse = responseFor(anchorVelocity, anchorTarget, profile);

        double oldYawVelocity = yawVelocity;
        double yawDecay = Math.exp(-yawResponse * stepSeconds);
        yawVelocity = yawTarget + (oldYawVelocity - yawTarget) * yawDecay;
        state.addYaw(yawTarget * stepSeconds
                + (oldYawVelocity - yawTarget) * (1.0 - yawDecay) / yawResponse);

        double oldAnchorVelocity = anchorVelocity;
        double anchorDecay = Math.exp(-anchorResponse * stepSeconds);
        anchorVelocity = anchorTarget + (oldAnchorVelocity - anchorTarget) * anchorDecay;
        state.addAnchorOffset(anchorTarget * stepSeconds
                + (oldAnchorVelocity - anchorTarget) * (1.0 - anchorDecay) / anchorResponse);

        if (input.forward() != 0.0) {
            state.cancelRadiusTarget();
            integrateRadiusVelocity(
                    state,
                    -input.forward() * RADIUS_SPEED,
                    profile,
                    stepSeconds
            );
        } else if (state.hasRadiusTarget()) {
            integrateRequestedRadius(state, profile, stepSeconds);
        } else {
            integrateRadiusVelocity(state, 0.0, profile, stepSeconds);
        }
    }

    private void integrateRadiusVelocity(
            OrbitCameraState state,
            double targetVelocity,
            CinematicMotionProfile profile,
            double stepSeconds
    ) {
        double response = responseFor(radiusVelocity, targetVelocity, profile);
        double oldVelocity = radiusVelocity;
        double decay = Math.exp(-response * stepSeconds);
        radiusVelocity = targetVelocity + (oldVelocity - targetVelocity) * decay;
        double displacement = targetVelocity * stepSeconds
                + (oldVelocity - targetVelocity) * (1.0 - decay) / response;
        if (state.setIntegratedRadius(state.radius() + displacement)) {
            radiusVelocity = 0.0;
        }
    }

    private void integrateRequestedRadius(
            OrbitCameraState state,
            CinematicMotionProfile profile,
            double stepSeconds
    ) {
        double response = profile.radiusTargetResponse();
        double decay = Math.exp(-response * stepSeconds);
        double nextRadius = state.requestedRadius()
                + (state.radius() - state.requestedRadius()) * decay;
        if (Math.abs(nextRadius - state.requestedRadius()) <= RADIUS_TARGET_EPSILON) {
            state.setIntegratedRadius(state.requestedRadius());
            state.cancelRadiusTarget();
            radiusVelocity = 0.0;
            return;
        }
        state.setIntegratedRadius(nextRadius);
        radiusVelocity = response * (state.requestedRadius() - nextRadius);
    }

    private static double responseFor(
            double velocity,
            double targetVelocity,
            CinematicMotionProfile profile
    ) {
        if (targetVelocity == 0.0) {
            return profile.decelerationResponse();
        }
        return velocity * targetVelocity < 0.0
                ? profile.reversalResponse()
                : profile.accelerationResponse();
    }

    private static boolean isAcceptedElapsedTime(double elapsedSeconds) {
        return Double.isFinite(elapsedSeconds)
                && elapsedSeconds > 0.0
                && elapsedSeconds <= MAX_ACCEPTED_ELAPSED_SECONDS;
    }
}
