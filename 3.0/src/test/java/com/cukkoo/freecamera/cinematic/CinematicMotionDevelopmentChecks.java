package com.cukkoo.freecamera.cinematic;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import net.minecraft.util.Mth;

public final class CinematicMotionDevelopmentChecks {
    private static final int[] FRAME_RATES = {10, 20, 30, 60, 144, 240};
    private static final double POSITION_TOLERANCE = 1.0E-8;
    private static final double ROTATION_TOLERANCE = 2.0E-4;

    private CinematicMotionDevelopmentChecks() {
    }

    public static void main(String[] args) {
        comparePositionAcrossFrameRates("acceleration from rest", PositionScenario.ACCELERATE);
        comparePositionAcrossFrameRates("deceleration to rest", PositionScenario.DECELERATE);
        comparePositionAcrossFrameRates("rapid direction reversal", PositionScenario.REVERSE);
        compareRotationAcrossFrameRates();
        verifyYawWrapping();
        verifyPitchClamping();
        verifyNoRotationDrift();
        verifyTogglePosePreservation();
        verifyScreenSuspension();
        verifyTripodPolicy();
        System.out.println("Cinematic motion checks passed at 10, 20, 30, 60, 144, and 240 FPS.");
    }

    private static void comparePositionAcrossFrameRates(String name, PositionScenario scenario) {
        PositionResult reference = simulatePosition(240, scenario);
        for (int fps : FRAME_RATES) {
            PositionResult result = simulatePosition(fps, scenario);
            require(result.maxDifference(reference) <= POSITION_TOLERANCE,
                    name + " differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
    }

    private static PositionResult simulatePosition(int fps, PositionScenario scenario) {
        CinematicPositionSmoother smoother = new CinematicPositionSmoother();
        double position = 0.0;
        double elapsed = 1.0 / fps;
        smoother.setTargetVelocity(4.0, 0.0, 0.0);
        for (int frame = 0; frame < fps; frame++) {
            position += integrateFrame(smoother, elapsed);
        }
        if (scenario == PositionScenario.DECELERATE) {
            smoother.setTargetVelocity(0.0, 0.0, 0.0);
            for (int frame = 0; frame < fps * 2; frame++) {
                position += integrateFrame(smoother, elapsed);
            }
        } else if (scenario == PositionScenario.REVERSE) {
            smoother.setTargetVelocity(-4.0, 0.0, 0.0);
            for (int frame = 0; frame < fps * 2; frame++) {
                position += integrateFrame(smoother, elapsed);
            }
        }
        return new PositionResult(position, smoother.velocityX());
    }

    private static double integrateFrame(CinematicPositionSmoother smoother, double elapsed) {
        int steps = Math.max(1, (int) Math.ceil(elapsed / 0.05));
        double remaining = elapsed;
        double displacement = 0.0;
        for (int step = 0; step < steps; step++) {
            double substep = Math.min(0.05, remaining);
            smoother.integrate(CinematicMotionProfile.CINEMATIC, substep);
            displacement += smoother.displacementX();
            remaining -= substep;
        }
        return displacement;
    }

    private static void compareRotationAcrossFrameRates() {
        RotationResult reference = simulateRotation(240);
        for (int fps : FRAME_RATES) {
            RotationResult result = simulateRotation(fps);
            require(result.maxDifference(reference) <= ROTATION_TOLERANCE,
                    "rotation differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
    }

    private static RotationResult simulateRotation(int fps) {
        CinematicRotationSmoother smoother = new CinematicRotationSmoother();
        smoother.initialize(15.0F, -10.0F);
        smoother.addTargetDelta(15.0F, -10.0F, 120.0, 45.0);
        double elapsed = 1.0 / fps;
        for (int frame = 0; frame < fps * 2; frame++) {
            smoother.advance(
                    smoother.outputYaw(),
                    smoother.outputPitch(),
                    CinematicMotionProfile.CINEMATIC,
                    elapsed
            );
        }
        return new RotationResult(
                smoother.outputYaw(),
                smoother.outputPitch(),
                smoother.yawVelocity(),
                smoother.pitchVelocity()
        );
    }

    private static void verifyYawWrapping() {
        CinematicRotationSmoother smoother = new CinematicRotationSmoother();
        smoother.initialize(179.0F, 0.0F);
        smoother.addTargetDelta(179.0F, 0.0F, 2.0, 0.0);
        smoother.advance(179.0F, 0.0F, CinematicMotionProfile.CINEMATIC, 0.05);
        double traveled = Mth.wrapDegrees(smoother.outputYaw() - 179.0F);
        require(traveled > 0.0 && traveled < 2.0,
                "yaw wrap did not use the shortest path: " + traveled);
        for (int step = 0; step < 200; step++) {
            smoother.advance(
                    smoother.outputYaw(),
                    smoother.outputPitch(),
                    CinematicMotionProfile.CINEMATIC,
                    0.05
            );
        }
        require(Math.abs(Mth.wrapDegrees(smoother.outputYaw() - -179.0F)) <= ROTATION_TOLERANCE,
                "yaw wrap did not settle at -179 degrees");
    }

    private static void verifyNoRotationDrift() {
        CinematicRotationSmoother smoother = new CinematicRotationSmoother();
        smoother.initialize(-73.0F, 24.0F);
        for (int step = 0; step < 300; step++) {
            smoother.advance(
                    smoother.outputYaw(),
                    smoother.outputPitch(),
                    CinematicMotionProfile.CINEMATIC,
                    1.0 / 60.0
            );
        }
        require(Math.abs(Mth.wrapDegrees(smoother.outputYaw() + 73.0F)) <= ROTATION_TOLERANCE,
                "zero mouse input accumulated yaw drift");
        require(Math.abs(smoother.outputPitch() - 24.0F) <= ROTATION_TOLERANCE,
                "zero mouse input accumulated pitch drift");
    }

    private static void verifyPitchClamping() {
        CinematicRotationSmoother smoother = new CinematicRotationSmoother();
        smoother.initialize(0.0F, 80.0F);
        smoother.addTargetDelta(0.0F, 80.0F, 0.0, 50.0);
        smoother.clampPitchTarget(-85.0, 85.0);
        for (int step = 0; step < 200; step++) {
            smoother.advance(
                    smoother.outputYaw(),
                    smoother.outputPitch(),
                    CinematicMotionProfile.CINEMATIC,
                    0.05
            );
        }
        require(smoother.outputPitch() <= 85.0F,
                "Orbit Cinematic pitch exceeded its pole limit");
    }

    private static void verifyTogglePosePreservation() {
        CinematicMotionState state = new CinematicMotionState();
        CameraPose pose = new CameraPose(3.0, 70.0, -8.0, 42.0F, -16.0F);
        PoseResult expected = PoseResult.capture(pose);
        require(state.toggle(CameraMode.FREE_CAMERA, pose.yaw(), pose.pitch()),
                "Free Camera cinematic enable was rejected");
        expected.requireMatches(pose, "enabling Cinematic changed the pose");
        require(state.toggle(CameraMode.FREE_CAMERA, pose.yaw(), pose.pitch()),
                "Free Camera cinematic disable was rejected");
        expected.requireMatches(pose, "disabling Cinematic changed the pose");
    }

    private static void verifyScreenSuspension() {
        CinematicMotionState state = new CinematicMotionState();
        state.toggle(CameraMode.ORBIT, 30.0F, -12.0F);
        CinematicRotationSmoother smoother = state.rotationSmoother();
        smoother.addTargetDelta(30.0F, -12.0F, 80.0, 20.0);
        smoother.advance(30.0F, -12.0F, CinematicMotionProfile.CINEMATIC, 0.1);
        float frozenYaw = smoother.outputYaw();
        float frozenPitch = smoother.outputPitch();
        state.suspend(frozenYaw, frozenPitch);
        require(!smoother.advance(
                        frozenYaw,
                        frozenPitch,
                        CinematicMotionProfile.CINEMATIC,
                        smoother.elapsedSeconds(1L)),
                "first frame after suspension advanced rotation");
        require(smoother.outputYaw() == frozenYaw && smoother.outputPitch() == frozenPitch,
                "screen suspension changed or resumed past the frozen pose");
    }

    private static void verifyTripodPolicy() {
        CinematicMotionState state = new CinematicMotionState();
        require(!state.toggle(CameraMode.TRIPOD, 0.0F, 0.0F),
                "Tripod accepted the Cinematic toggle");
        require(!state.isEnabled(), "Tripod enabled Cinematic state");
        require(state.activeProfile(CameraMode.TRIPOD) == CinematicMotionProfile.RESPONSIVE,
                "Tripod received Cinematic position smoothing");
        require(state.activeProfile(null) == CinematicMotionProfile.RESPONSIVE,
                "inactive state received Cinematic smoothing");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private enum PositionScenario {
        ACCELERATE,
        DECELERATE,
        REVERSE
    }

    private record PositionResult(double position, double velocity) {
        double maxDifference(PositionResult other) {
            return Math.max(Math.abs(position - other.position), Math.abs(velocity - other.velocity));
        }
    }

    private record RotationResult(double yaw, double pitch, double yawVelocity, double pitchVelocity) {
        double maxDifference(RotationResult other) {
            return Math.max(
                    Math.max(Math.abs(Mth.wrapDegrees(yaw - other.yaw)), Math.abs(pitch - other.pitch)),
                    Math.max(Math.abs(yawVelocity - other.yawVelocity),
                            Math.abs(pitchVelocity - other.pitchVelocity))
            );
        }
    }

    private record PoseResult(double x, double y, double z, float yaw, float pitch) {
        static PoseResult capture(CameraPose pose) {
            return new PoseResult(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
        }

        void requireMatches(CameraPose pose, String message) {
            require(x == pose.x() && y == pose.y() && z == pose.z()
                            && yaw == pose.yaw() && pitch == pose.pitch(),
                    message);
        }
    }
}
