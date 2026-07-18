package com.cukkoo.freecamera.motion;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;

public final class CameraMotionIntegratorDevelopmentChecks {
    private static final int[] FRAME_RATES = {10, 20, 30, 60, 144, 240};
    private static final double CONSISTENCY_TOLERANCE = 1.0E-8;
    private static final double DIRECTION_TOLERANCE = 1.0E-7;

    private CameraMotionIntegratorDevelopmentChecks() {
    }

    public static void main(String[] args) {
        verifySubstepBounds();
        compareAcrossFrameRates("constant forward", Scenario.CONSTANT_FORWARD);
        compareAcrossFrameRates("diagonal normalized", Scenario.DIAGONAL);
        compareAcrossFrameRates("acceleration from rest", Scenario.ACCELERATION);
        compareAcrossFrameRates("deceleration after release", Scenario.DECELERATION);
        compareAcrossFrameRates(
                "cinematic acceleration",
                Scenario.ACCELERATION,
                CinematicMotionProfile.CINEMATIC
        );
        compareAcrossFrameRates(
                "cinematic deceleration",
                Scenario.DECELERATION,
                CinematicMotionProfile.CINEMATIC
        );
        verifyStrafeDirections();
        verifyNormalizedDiagonals();
        System.out.println("Camera motion consistency checks passed at 10, 20, 30, 60, 144, and 240 FPS.");
    }

    private static void verifySubstepBounds() {
        double covered = CameraMotionIntegrator.MAX_SUBSTEP_SECONDS
                * CameraMotionIntegrator.MAX_SUBSTEP_COUNT;
        require(covered + CONSISTENCY_TOLERANCE
                        >= CameraMotionIntegrator.MAX_ACCEPTED_ELAPSED_SECONDS,
                "Maximum substeps do not cover the accepted elapsed-time limit");
    }

    private static void compareAcrossFrameRates(String name, Scenario scenario) {
        compareAcrossFrameRates(name, scenario, CinematicMotionProfile.RESPONSIVE);
    }

    private static void compareAcrossFrameRates(
            String name,
            Scenario scenario,
            CinematicMotionProfile profile
    ) {
        MotionResult reference = simulate(240, scenario, profile);
        double largestDifference = 0.0;
        for (int fps : FRAME_RATES) {
            MotionResult result = simulate(fps, scenario, profile);
            largestDifference = Math.max(largestDifference, result.maxDifference(reference));
            require(result.maxDifference(reference) <= CONSISTENCY_TOLERANCE,
                    name + " differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
        System.out.printf("%-28s max difference %.3e%n", name, largestDifference);
    }

    private static MotionResult simulate(int fps, Scenario scenario) {
        return simulate(fps, scenario, CinematicMotionProfile.RESPONSIVE);
    }

    private static MotionResult simulate(
            int fps,
            Scenario scenario,
            CinematicMotionProfile profile
    ) {
        CameraMotionIntegrator integrator = new CameraMotionIntegrator();
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        CameraInputSnapshot input = new CameraInputSnapshot();
        double frameSeconds = 1.0 / fps;

        switch (scenario) {
            case CONSTANT_FORWARD -> {
                input.set(1.0, 0.0, 0.0);
                advanceFrames(integrator, pose, input, profile, fps * 3, frameSeconds);
            }
            case DIAGONAL -> {
                input.set(1.0, 1.0, 0.0);
                advanceFrames(integrator, pose, input, profile, fps * 3, frameSeconds);
            }
            case ACCELERATION -> {
                input.set(1.0, 0.0, 0.0);
                advanceFrames(integrator, pose, input, profile, fps, frameSeconds);
            }
            case DECELERATION -> {
                input.set(1.0, 0.0, 0.0);
                advanceFrames(integrator, pose, input, profile, fps, frameSeconds);
                input.clear();
                advanceFrames(integrator, pose, input, profile, fps, frameSeconds);
            }
        }

        return result(integrator, pose);
    }

    private static void verifyStrafeDirections() {
        float[] yaws = {0.0F, 90.0F, -90.0F};
        for (float yaw : yaws) {
            verifyStrafeAt(yaw, 0.0F, -1.0, "A");
            verifyStrafeAt(yaw, 0.0F, 1.0, "D");
        }
        verifyStrafeAt(37.0F, 55.0F, -1.0, "A with pitch");
        verifyStrafeAt(37.0F, -55.0F, 1.0, "D with pitch");
    }

    private static void verifyStrafeAt(float yaw, float pitch, double strafe, String key) {
        CameraMotionIntegrator integrator = new CameraMotionIntegrator();
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, yaw, pitch);
        CameraInputSnapshot input = new CameraInputSnapshot();
        input.set(0.0, strafe, 0.0);
        advanceFrames(integrator, pose, input, 60, 1.0 / 60.0);

        double yawRadians = Math.toRadians(yaw);
        double cameraRightX = -Math.cos(yawRadians);
        double cameraRightZ = -Math.sin(yawRadians);
        double rightProjection = pose.x() * cameraRightX + pose.z() * cameraRightZ;
        require(strafe > 0.0 ? rightProjection > 0.0 : rightProjection < 0.0,
                key + " strafe direction is reversed at yaw " + yaw + " pitch " + pitch);
        require(Math.abs(pose.y()) <= DIRECTION_TOLERANCE,
                key + " horizontal strafe changed vertical position");
    }

    private static void verifyNormalizedDiagonals() {
        double[][] axes = {
                {1.0, -1.0},
                {1.0, 1.0},
                {-1.0, -1.0},
                {-1.0, 1.0}
        };
        for (double[] axis : axes) {
            CameraMotionIntegrator integrator = new CameraMotionIntegrator();
            CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 23.0F, 31.0F);
            CameraInputSnapshot input = new CameraInputSnapshot();
            input.set(axis[0], axis[1], 0.0);
            advanceFrames(integrator, pose, input, 300, 1.0 / 60.0);
            double speed = Math.sqrt(
                    square(integrator.velocityXForDevelopmentCheck())
                            + square(integrator.velocityYForDevelopmentCheck())
                            + square(integrator.velocityZForDevelopmentCheck())
            );
            require(Math.abs(speed - SpeedPreset.NORMAL.blocksPerSecond()) <= DIRECTION_TOLERANCE,
                    "Diagonal speed was not normalized for forward=" + axis[0] + " strafe=" + axis[1]);
        }
    }

    private static void advanceFrames(
            CameraMotionIntegrator integrator,
            CameraPose pose,
            CameraInputSnapshot input,
            int frames,
            double frameSeconds
    ) {
        advanceFrames(
                integrator,
                pose,
                input,
                CinematicMotionProfile.RESPONSIVE,
                frames,
                frameSeconds
        );
    }

    private static void advanceFrames(
            CameraMotionIntegrator integrator,
            CameraPose pose,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            int frames,
            double frameSeconds
    ) {
        for (int frame = 0; frame < frames; frame++) {
            integrator.advanceForDevelopmentCheck(pose, input, profile, frameSeconds);
        }
    }

    private static MotionResult result(CameraMotionIntegrator integrator, CameraPose pose) {
        return new MotionResult(
                pose.x(),
                pose.y(),
                pose.z(),
                integrator.velocityXForDevelopmentCheck(),
                integrator.velocityYForDevelopmentCheck(),
                integrator.velocityZForDevelopmentCheck()
        );
    }

    private static double square(double value) {
        return value * value;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private enum Scenario {
        CONSTANT_FORWARD,
        DIAGONAL,
        ACCELERATION,
        DECELERATION
    }

    private record MotionResult(
            double x,
            double y,
            double z,
            double velocityX,
            double velocityY,
            double velocityZ
    ) {
        double maxDifference(MotionResult other) {
            return Math.max(
                    Math.max(Math.max(Math.abs(x - other.x), Math.abs(y - other.y)),
                            Math.abs(z - other.z)),
                    Math.max(Math.max(Math.abs(velocityX - other.velocityX),
                            Math.abs(velocityY - other.velocityY)),
                            Math.abs(velocityZ - other.velocityZ))
            );
        }
    }
}
