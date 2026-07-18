package com.cukkoo.freecamera.orbit;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import com.cukkoo.freecamera.tripod.TripodCameraRig;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;

public final class OrbitCameraDevelopmentChecks {
    private static final int[] FRAME_RATES = {10, 20, 30, 60, 144, 240};
    private static final double TOLERANCE = 1.0E-8;
    private static final double DIRECTION_TOLERANCE = 1.0E-6;

    private OrbitCameraDevelopmentChecks() {
    }

    public static void main(String[] args) {
        verifySubstepBounds();
        verifyCardinalOrbitPositions();
        verifyClamps();
        verifyFreeToOrbitConversion();
        verifyOrbitToFreePosePreservation();
        verifyTripodToOrbitConversion();
        verifyAnchorFollowing();
        verifyStrafeDirection();
        verifyScreenSuspensionPreservesPoseState();
        compareAcrossFrameRates("angular acceleration", -1.0, 0.0, 0.0, false);
        compareAcrossFrameRates("radius acceleration", 0.0, 1.0, 0.0, false);
        compareAcrossFrameRates("anchor acceleration", 0.0, 0.0, 1.0, false);
        compareAcrossFrameRates("deceleration", -1.0, 1.0, 1.0, true);
        compareAcrossFrameRates(
                "cinematic orbit motion",
                -1.0,
                1.0,
                1.0,
                true,
                CinematicMotionProfile.CINEMATIC
        );
        System.out.println("Orbit Camera checks passed at 10, 20, 30, 60, 144, and 240 FPS.");
    }

    private static void verifySubstepBounds() {
        require(OrbitMotionIntegrator.MAX_SUBSTEP_SECONDS
                        * OrbitMotionIntegrator.MAX_SUBSTEP_COUNT + TOLERANCE
                        >= OrbitMotionIntegrator.MAX_ACCEPTED_ELAPSED_SECONDS,
                "Orbit substeps do not cover the accepted elapsed-time limit");
    }

    private static void verifyCardinalOrbitPositions() {
        verifyPosition(0.0F, 0.0, 0.0, 4.0);
        verifyPosition(90.0F, -4.0, 0.0, 0.0);
        verifyPosition(180.0F, 0.0, 0.0, -4.0);
        verifyPosition(-90.0F, 4.0, 0.0, 0.0);
    }

    private static void verifyPosition(float yaw, double x, double y, double z) {
        OrbitCameraRig rig = new OrbitCameraRig();
        rig.state().initialize(yaw, 0.0F, 4.0, 0.0);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        rig.applyPose(pose, 0.0, 0.0, 0.0);
        requireClose(pose.x(), x, "orbit X at yaw " + yaw);
        requireClose(pose.y(), y, "orbit Y at yaw " + yaw);
        requireClose(pose.z(), z, "orbit Z at yaw " + yaw);
    }

    private static void verifyClamps() {
        OrbitCameraState state = new OrbitCameraState();
        state.initialize(0.0F, 100.0F, 0.0, 0.0);
        requireClose(state.pitch(), OrbitCameraState.MAX_VERTICAL_ANGLE, "positive pitch clamp");
        requireClose(state.radius(), OrbitCameraState.MIN_RADIUS, "minimum radius clamp");
        state.initialize(0.0F, -100.0F, 100.0, 0.0);
        requireClose(state.pitch(), -OrbitCameraState.MAX_VERTICAL_ANGLE, "negative pitch clamp");
        requireClose(state.radius(), OrbitCameraState.MAX_RADIUS, "maximum radius clamp");
    }

    private static void verifyFreeToOrbitConversion() {
        OrbitCameraRig source = new OrbitCameraRig();
        source.state().initialize(37.0F, -24.0F, 11.0, 0.0);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        source.applyPose(pose, 12.0, 70.0, -8.0);
        PoseResult expected = PoseResult.capture(pose);

        OrbitCameraRig converted = new OrbitCameraRig();
        converted.enterFromPose(pose, 12.0, 70.0, -8.0, 0.0F);
        expected.requireMatches(pose, "Free-to-Orbit conversion changed the rendered pose");
    }

    private static void verifyOrbitToFreePosePreservation() {
        OrbitCameraRig rig = new OrbitCameraRig();
        rig.state().initialize(-61.0F, 33.0F, 9.0, 2.0);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        rig.applyPose(pose, 4.0, 18.0, 7.0);
        PoseResult expected = PoseResult.capture(pose);
        rig.leaveToFree();
        expected.requireMatches(pose, "Orbit-to-Free transition changed the rendered pose");
    }

    private static void verifyTripodToOrbitConversion() {
        OrbitCameraRig source = new OrbitCameraRig();
        source.state().initialize(-42.0F, 28.0F, 12.0, 0.0);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        source.applyPose(pose, 6.0, 68.0, -11.0);

        TripodCameraRig tripod = new TripodCameraRig();
        tripod.enterFromPose(pose);
        PoseResult expected = PoseResult.capture(pose);

        OrbitCameraRig converted = new OrbitCameraRig();
        converted.enterFromPose(pose, 6.0, 68.0, -11.0, 0.0F);
        tripod.clear();
        expected.requireMatches(pose, "Tripod-to-Orbit conversion changed an in-range pose");
        require(!tripod.state().isCaptured(), "Tripod-to-Orbit conversion retained Tripod state");
    }

    private static void verifyAnchorFollowing() {
        OrbitCameraRig rig = new OrbitCameraRig();
        rig.state().initialize(0.0F, 0.0F, 4.0, 1.5);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        rig.applyPose(pose, 1.0, 2.0, 3.0);
        requireClose(pose.x(), 1.0, "followed anchor X");
        requireClose(pose.y(), 3.5, "followed anchor Y plus offset");
        requireClose(pose.z(), 7.0, "followed anchor Z plus radius");
    }

    private static void verifyStrafeDirection() {
        OrbitResult left = simulate(60, -1.0, 0.0, 0.0, false);
        OrbitResult right = simulate(60, 1.0, 0.0, 0.0, false);
        require(left.yaw > 0.0, "A must orbit toward positive yaw");
        require(right.yaw < 0.0, "D must orbit toward negative yaw");
    }

    private static void verifyScreenSuspensionPreservesPoseState() {
        OrbitCameraRig rig = new OrbitCameraRig();
        rig.state().initialize(48.0F, -17.0F, 13.0, 2.5);
        CameraInputSnapshot input = new CameraInputSnapshot();
        input.set(1.0, -1.0, 1.0);
        rig.motionIntegratorForDevelopmentCheck()
                .advanceForDevelopmentCheck(rig.state(), input, 0.1);
        CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
        rig.applyPose(pose, 3.0, 20.0, -4.0);
        PoseResult expectedPose = PoseResult.capture(pose);
        double expectedYaw = rig.state().yaw();
        double expectedPitch = rig.state().pitch();
        double expectedRadius = rig.state().radius();
        double expectedAnchorOffset = rig.state().anchorOffsetY();
        rig.suspendMotion();
        expectedPose.requireMatches(pose, "screen suspension changed the camera pose");
        requireClose(rig.state().yaw(), expectedYaw, "screen suspension changed Orbit yaw");
        requireClose(rig.state().pitch(), expectedPitch, "screen suspension changed Orbit pitch");
        requireClose(rig.state().radius(), expectedRadius, "screen suspension changed Orbit radius");
        requireClose(rig.state().anchorOffsetY(), expectedAnchorOffset,
                "screen suspension changed Orbit anchor offset");
        OrbitMotionIntegrator integrator = rig.motionIntegratorForDevelopmentCheck();
        requireClose(integrator.yawVelocityForDevelopmentCheck(), 0.0,
                "screen suspension retained yaw velocity");
        requireClose(integrator.radiusVelocityForDevelopmentCheck(), 0.0,
                "screen suspension retained radius velocity");
        requireClose(integrator.anchorVelocityForDevelopmentCheck(), 0.0,
                "screen suspension retained anchor velocity");
    }

    private static void compareAcrossFrameRates(
            String name,
            double strafe,
            double forward,
            double vertical,
            boolean release
    ) {
        compareAcrossFrameRates(
                name,
                strafe,
                forward,
                vertical,
                release,
                CinematicMotionProfile.RESPONSIVE
        );
    }

    private static void compareAcrossFrameRates(
            String name,
            double strafe,
            double forward,
            double vertical,
            boolean release,
            CinematicMotionProfile profile
    ) {
        OrbitResult reference = simulate(240, strafe, forward, vertical, release, profile);
        for (int fps : FRAME_RATES) {
            OrbitResult result = simulate(fps, strafe, forward, vertical, release, profile);
            require(result.maxDifference(reference) <= DIRECTION_TOLERANCE,
                    name + " differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
    }

    private static OrbitResult simulate(
            int fps,
            double strafe,
            double forward,
            double vertical,
            boolean release
    ) {
        return simulate(
                fps,
                strafe,
                forward,
                vertical,
                release,
                CinematicMotionProfile.RESPONSIVE
        );
    }

    private static OrbitResult simulate(
            int fps,
            double strafe,
            double forward,
            double vertical,
            boolean release,
            CinematicMotionProfile profile
    ) {
        OrbitCameraRig rig = new OrbitCameraRig();
        rig.state().initialize(0.0F, 0.0F, 32.0, 0.0);
        OrbitMotionIntegrator integrator = rig.motionIntegratorForDevelopmentCheck();
        CameraInputSnapshot input = new CameraInputSnapshot();
        input.set(forward, strafe, vertical);
        advance(integrator, rig.state(), input, profile, fps, 1.0 / fps);
        if (release) {
            input.clear();
            advance(integrator, rig.state(), input, profile, fps, 1.0 / fps);
        }
        return OrbitResult.capture(rig);
    }

    private static void advance(
            OrbitMotionIntegrator integrator,
            OrbitCameraState state,
            CameraInputSnapshot input,
            int frames,
            double elapsed
    ) {
        advance(
                integrator,
                state,
                input,
                CinematicMotionProfile.RESPONSIVE,
                frames,
                elapsed
        );
    }

    private static void advance(
            OrbitMotionIntegrator integrator,
            OrbitCameraState state,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            int frames,
            double elapsed
    ) {
        for (int frame = 0; frame < frames; frame++) {
            integrator.advanceForDevelopmentCheck(state, input, profile, elapsed);
        }
    }

    private static void requireClose(double actual, double expected, String name) {
        require(Math.abs(actual - expected) <= DIRECTION_TOLERANCE,
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

        void requireMatches(CameraPose pose, String message) {
            require(maxDifference(capture(pose)) <= DIRECTION_TOLERANCE, message);
        }

        double maxDifference(PoseResult other) {
            return Math.max(
                    Math.max(Math.max(Math.abs(x - other.x), Math.abs(y - other.y)),
                            Math.abs(z - other.z)),
                    Math.max(Math.abs(yaw - other.yaw), Math.abs(pitch - other.pitch))
            );
        }
    }

    private record OrbitResult(
            double yaw,
            double pitch,
            double radius,
            double anchorOffset,
            double yawVelocity,
            double radiusVelocity,
            double anchorVelocity
    ) {
        static OrbitResult capture(OrbitCameraRig rig) {
            OrbitCameraState state = rig.state();
            OrbitMotionIntegrator integrator = rig.motionIntegratorForDevelopmentCheck();
            return new OrbitResult(
                    state.yaw(),
                    state.pitch(),
                    state.radius(),
                    state.anchorOffsetY(),
                    integrator.yawVelocityForDevelopmentCheck(),
                    integrator.radiusVelocityForDevelopmentCheck(),
                    integrator.anchorVelocityForDevelopmentCheck()
            );
        }

        double maxDifference(OrbitResult other) {
            return Math.max(
                    Math.max(Math.max(Math.abs(yaw - other.yaw), Math.abs(pitch - other.pitch)),
                            Math.max(Math.abs(radius - other.radius), Math.abs(anchorOffset - other.anchorOffset))),
                    Math.max(Math.max(Math.abs(yawVelocity - other.yawVelocity),
                            Math.abs(radiusVelocity - other.radiusVelocity)),
                            Math.abs(anchorVelocity - other.anchorVelocity))
            );
        }
    }
}
