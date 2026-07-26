package com.cukkoo.freecamera.tripod;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import com.cukkoo.freecamera.roll.CameraRollController;

public final class TripodCameraDevelopmentChecks {
    private static final double TOLERANCE = 1.0E-9;

    private TripodCameraDevelopmentChecks() {
    }

    public static void main(String[] args) {
        verifyEntryPreservesPose("Free to Tripod");
        verifyEntryPreservesPose("Orbit to Tripod");
        verifyTripodToFreePreservesPose();
        verifyPlayerMovementCannotAffectPose();
        verifyScreenSuspensionModelPreservesState();
        verifyRollResetPreservesTripodPose();
        verifyCleanupClearsState();
        System.out.println("Tripod Camera pose, suspension, and cleanup checks passed.");
    }

    private static void verifyEntryPreservesPose(String scenario) {
        TripodCameraRig rig = new TripodCameraRig();
        CameraPose pose = samplePose();
        PoseResult expected = PoseResult.capture(pose);
        rig.enterFromPose(pose);
        expected.requireMatches(pose, scenario + " changed the camera pose");
        require(rig.state().isCaptured(), scenario + " did not capture state");
    }

    private static void verifyTripodToFreePreservesPose() {
        TripodCameraRig rig = new TripodCameraRig();
        CameraPose pose = samplePose();
        rig.enterFromPose(pose);
        PoseResult expected = PoseResult.capture(pose);
        rig.leave();
        expected.requireMatches(pose, "Tripod to Free changed the camera pose");
        require(!rig.state().isCaptured(), "Tripod to Free retained Tripod state");
    }

    private static void verifyPlayerMovementCannotAffectPose() {
        TripodCameraRig rig = new TripodCameraRig();
        CameraPose pose = samplePose();
        rig.enterFromPose(pose);
        PoseResult expected = PoseResult.capture(pose);

        // Simulate arbitrary external camera/player-follow contamination between render passes.
        pose.set(-900.0, 400.0, 1200.0, -175.0F, 80.0F);
        rig.applyPose(pose);
        expected.requireMatches(pose, "external player movement affected the Tripod pose");
    }

    private static void verifyScreenSuspensionModelPreservesState() {
        TripodCameraRig rig = new TripodCameraRig();
        CameraPose pose = samplePose();
        rig.enterFromPose(pose);
        PoseResult expectedPose = PoseResult.capture(pose);
        PoseResult expectedState = PoseResult.capture(rig.state());

        // Tripod has no timing or input state to advance or clear while a screen is open.
        rig.applyPose(pose);
        expectedPose.requireMatches(pose, "screen suspension changed Tripod pose");
        expectedState.requireMatches(rig.state(), "screen suspension changed Tripod state");
    }

    private static void verifyCleanupClearsState() {
        TripodCameraRig rig = new TripodCameraRig();
        rig.enterFromPose(samplePose());
        rig.clear();
        require(!rig.state().isCaptured(), "cleanup retained captured Tripod state");
        requireClose(rig.state().x(), 0.0, "cleanup X");
        requireClose(rig.state().y(), 0.0, "cleanup Y");
        requireClose(rig.state().z(), 0.0, "cleanup Z");
        requireClose(rig.state().yaw(), 0.0, "cleanup yaw");
        requireClose(rig.state().pitch(), 0.0, "cleanup pitch");
    }

    private static void verifyRollResetPreservesTripodPose() {
        TripodCameraRig rig = new TripodCameraRig();
        CameraPose pose = samplePose();
        rig.enterFromPose(pose);
        PoseResult expectedPose = PoseResult.capture(pose);
        PoseResult expectedState = PoseResult.capture(rig.state());
        CameraRollController roll = new CameraRollController();
        for (int frame = 0; frame < 120; frame++) {
            roll.advanceForDevelopmentCheck(
                    CinematicMotionProfile.CINEMATIC,
                    1.0 / 60.0,
                    1
            );
        }
        require(Math.abs(roll.state().currentRoll()) > 1.0,
                "Tripod reset test did not establish roll");
        roll.resetToZero();
        rig.applyPose(pose);
        requireClose(roll.state().currentRoll(), 0.0, "Tripod R roll reset");
        expectedPose.requireMatches(pose, "Tripod R changed its rendered pose");
        expectedState.requireMatches(rig.state(), "Tripod R changed its captured pose");
    }

    private static CameraPose samplePose() {
        return new CameraPose(13.25, 71.5, -29.75, 127.5F, -38.25F);
    }

    private static void requireClose(double actual, double expected, String name) {
        require(Math.abs(actual - expected) <= TOLERANCE,
                name + ": expected " + expected + ", got " + actual);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record PoseResult(double x, double y, double z, float yaw, float pitch) {
        static PoseResult capture(CameraPose pose) {
            return new PoseResult(pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch());
        }

        static PoseResult capture(TripodCameraState state) {
            return new PoseResult(state.x(), state.y(), state.z(), state.yaw(), state.pitch());
        }

        void requireMatches(CameraPose pose, String message) {
            requireMatches(capture(pose), message);
        }

        void requireMatches(TripodCameraState state, String message) {
            requireMatches(capture(state), message);
        }

        private void requireMatches(PoseResult other, String message) {
            require(Math.abs(x - other.x) <= TOLERANCE
                            && Math.abs(y - other.y) <= TOLERANCE
                            && Math.abs(z - other.z) <= TOLERANCE
                            && Math.abs(yaw - other.yaw) <= TOLERANCE
                            && Math.abs(pitch - other.pitch) <= TOLERANCE,
                    message);
        }
    }
}
