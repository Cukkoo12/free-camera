package com.cukkoo.freecamera.cinematic;

import net.minecraft.util.Mth;

public final class CinematicRotationSmoother {
    private static final double ANGLE_EPSILON = 1.0E-5;
    private static final double VELOCITY_EPSILON = 1.0E-5;

    private double outputYaw;
    private double outputPitch;
    private double targetYaw;
    private double targetPitch;
    private double yawVelocity;
    private double pitchVelocity;
    private boolean initialized;
    private long lastFrameNanos;

    public void initialize(float yaw, float pitch) {
        outputYaw = Mth.wrapDegrees((double) yaw);
        outputPitch = Math.clamp((double) pitch, -90.0, 90.0);
        targetYaw = outputYaw;
        targetPitch = outputPitch;
        yawVelocity = 0.0;
        pitchVelocity = 0.0;
        initialized = true;
        lastFrameNanos = 0L;
    }

    public void addTargetDelta(float currentYaw, float currentPitch, double yawDelta, double pitchDelta) {
        ensureInitialized(currentYaw, currentPitch);
        synchronizeExternalRotation(currentYaw, currentPitch);
        targetYaw = Mth.wrapDegrees(targetYaw + finiteOrZero(yawDelta));
        targetPitch = Math.clamp(targetPitch + finiteOrZero(pitchDelta), -90.0, 90.0);
    }

    public void clampPitchTarget(double minimum, double maximum) {
        targetPitch = Math.clamp(targetPitch, minimum, maximum);
        outputPitch = Math.clamp(outputPitch, minimum, maximum);
        if ((outputPitch == minimum && pitchVelocity < 0.0)
                || (outputPitch == maximum && pitchVelocity > 0.0)) {
            pitchVelocity = 0.0;
        }
    }

    public boolean advance(
            float currentYaw,
            float currentPitch,
            CinematicMotionProfile profile,
            double elapsedSeconds
    ) {
        ensureInitialized(currentYaw, currentPitch);
        synchronizeExternalRotation(currentYaw, currentPitch);
        if (elapsedSeconds == 0.0) {
            return false;
        }
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            suspendAt(currentYaw, currentPitch);
            return false;
        }
        integrateAxis(profile.rotationFrequency(), elapsedSeconds);
        return true;
    }

    public float outputYaw() {
        return (float) Mth.wrapDegrees(outputYaw);
    }

    public float outputPitch() {
        return (float) Math.clamp(outputPitch, -90.0, 90.0);
    }

    public double yawVelocity() {
        return yawVelocity;
    }

    public double pitchVelocity() {
        return pitchVelocity;
    }

    public void suspendAt(float yaw, float pitch) {
        initialize(yaw, pitch);
    }

    public void clear() {
        outputYaw = 0.0;
        outputPitch = 0.0;
        targetYaw = 0.0;
        targetPitch = 0.0;
        yawVelocity = 0.0;
        pitchVelocity = 0.0;
        initialized = false;
        lastFrameNanos = 0L;
    }

    public double elapsedSeconds(long nowNanos) {
        if (lastFrameNanos == 0L) {
            lastFrameNanos = nowNanos;
            return 0.0;
        }
        double elapsed = (nowNanos - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = nowNanos;
        return elapsed;
    }

    private void integrateAxis(double frequency, double elapsedSeconds) {
        double yawError = shortestDelta(targetYaw, outputYaw);
        double yawCoefficient = yawVelocity + frequency * yawError;
        double decay = Math.exp(-frequency * elapsedSeconds);
        double nextYawError = (yawError + yawCoefficient * elapsedSeconds) * decay;
        yawVelocity = (yawVelocity - yawCoefficient * frequency * elapsedSeconds) * decay;
        outputYaw = Mth.wrapDegrees(targetYaw + nextYawError);

        double pitchError = outputPitch - targetPitch;
        double pitchCoefficient = pitchVelocity + frequency * pitchError;
        double nextPitchError = (pitchError + pitchCoefficient * elapsedSeconds) * decay;
        pitchVelocity = (pitchVelocity - pitchCoefficient * frequency * elapsedSeconds) * decay;
        outputPitch = Math.clamp(targetPitch + nextPitchError, -90.0, 90.0);
        settleIfClose();
    }

    private void synchronizeExternalRotation(float currentYaw, float currentPitch) {
        double externalYawDelta = shortestDelta(outputYaw, currentYaw);
        double externalPitchDelta = currentPitch - outputPitch;
        if (Math.abs(externalYawDelta) > ANGLE_EPSILON) {
            outputYaw = Mth.wrapDegrees(outputYaw + externalYawDelta);
            targetYaw = Mth.wrapDegrees(targetYaw + externalYawDelta);
        }
        if (Math.abs(externalPitchDelta) > ANGLE_EPSILON) {
            outputPitch = Math.clamp(outputPitch + externalPitchDelta, -90.0, 90.0);
            targetPitch = Math.clamp(targetPitch + externalPitchDelta, -90.0, 90.0);
        }
    }

    private void settleIfClose() {
        if (Math.abs(shortestDelta(outputYaw, targetYaw)) <= ANGLE_EPSILON
                && Math.abs(yawVelocity) <= VELOCITY_EPSILON) {
            outputYaw = targetYaw;
            yawVelocity = 0.0;
        }
        if (Math.abs(outputPitch - targetPitch) <= ANGLE_EPSILON
                && Math.abs(pitchVelocity) <= VELOCITY_EPSILON) {
            outputPitch = targetPitch;
            pitchVelocity = 0.0;
        }
    }

    private void ensureInitialized(float yaw, float pitch) {
        if (!initialized) {
            initialize(yaw, pitch);
        }
    }

    private static double shortestDelta(double from, double to) {
        return Mth.wrapDegrees(to - from);
    }

    private static boolean isAcceptedElapsedTime(double elapsedSeconds) {
        return Double.isFinite(elapsedSeconds) && elapsedSeconds > 0.0 && elapsedSeconds <= 0.25;
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
}
