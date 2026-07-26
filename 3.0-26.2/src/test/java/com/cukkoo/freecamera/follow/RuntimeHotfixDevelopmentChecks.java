package com.cukkoo.freecamera.follow;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import com.cukkoo.freecamera.render.DetachedCameraFov;
import com.cukkoo.freecamera.roll.CameraResetPolicy;

public final class RuntimeHotfixDevelopmentChecks {
    public static void main(String[] args) {
        verifyTwoStageResetPolicy();
        verifyDetachedFovSelection();
        verifyProfileGeometry();
        verifyFollowControls();
        verifyLookAtFpsConsistency();
        System.out.println("Runtime hotfix checks passed: reset, FOV, Follow profiles, controls, and FPS consistency.");
    }

    private static void verifyTwoStageResetPolicy() {
        for (CameraMode ignored : CameraMode.values()) {
            require(CameraResetPolicy.shouldResetRoll(5.0, 0.0), "nonzero roll must win reset priority");
            require(CameraResetPolicy.shouldResetRoll(0.0, 0.5), "angular velocity must win reset priority");
            require(!CameraResetPolicy.shouldResetRoll(1.0E-6, 1.0E-6), "upright camera must use mode reset");
        }
    }

    private static void verifyDetachedFovSelection() {
        require(close(DetachedCameraFov.select(true, 80.0F, 96.0F), 80.0), "active sprint FOV leaked");
        require(close(DetachedCameraFov.select(false, 80.0F, 96.0F), 96.0), "inactive vanilla FOV changed");
    }

    private static void verifyProfileGeometry() {
        float chase = FollowProfileMath.bearing(FollowProfile.CHASE, 30.0F, 0.0F);
        float side = FollowProfileMath.bearing(FollowProfile.SIDE, 30.0F, 0.0F);
        float fixed = FollowProfileMath.bearing(FollowProfile.FIXED_ANGLE, 30.0F, 0.0F);
        double chaseX = FollowProfileMath.desiredX(0.0, 6.0, 0.0, chase);
        double sideX = FollowProfileMath.desiredX(0.0, 6.0, 0.0, side);
        double fixedX = FollowProfileMath.desiredX(0.0, 6.0, 0.0, fixed);
        require(!close(chaseX, sideX) && !close(sideX, fixedX), "offset profiles are not distinct");
        require(!close(FollowProfileMath.bearing(FollowProfile.CHASE, 70.0F, 0.0F), chase), "CHASE must follow target bearing");
        require(close(FollowProfileMath.bearing(FollowProfile.FIXED_ANGLE, 70.0F, 0.0F), fixed), "FIXED_ANGLE followed target rotation");
        float rebuilt = FollowProfileMath.framingFromWorldBearing(FollowProfile.SIDE, 125.0F, 35.0F);
        require(close(FollowProfileMath.bearing(FollowProfile.SIDE, 35.0F, rebuilt), 125.0), "profile switch did not preserve bearing");
    }

    private static void verifyFollowControls() {
        FollowCameraState state = new FollowCameraState();
        double original = state.distance();
        state.adjustDistance(-1.0);
        require(state.distance() < original, "W must move closer");
        state.adjustDistance(2.0);
        require(state.distance() > original, "S must move farther");
        state.adjustBearing(10.0);
        require(state.framingYaw() > 0.0F, "A must orbit screen-left");
        state.adjustBearing(-20.0);
        require(state.framingYaw() < 0.0F, "D must orbit screen-right");
        require(FollowProfileMath.distanceDelta(1.0, 0.1, 8.0) < 0.0, "W mapping is not closer");
        require(FollowProfileMath.distanceDelta(-1.0, 0.1, 8.0) > 0.0, "S mapping is not farther");
        require(FollowProfileMath.bearingDelta(-1.0, 0.1, 65.0) > 0.0, "A mapping is reversed");
        require(FollowProfileMath.bearingDelta(1.0, 0.1, 65.0) < 0.0, "D mapping is reversed");
    }

    private static void verifyLookAtFpsConsistency() {
        double reference = Double.NaN;
        for (int fps : new int[]{10, 20, 30, 60, 144, 240}) {
            CameraPose pose = new CameraPose(0.0, 0.0, 0.0, 0.0F, 0.0F);
            CameraInputSnapshot input = new CameraInputSnapshot();
            input.set(1.0, 1.0, 0.0);
            FollowMotionIntegrator integrator = new FollowMotionIntegrator();
            int frames = fps * 4;
            for (int frame = 0; frame < frames; frame++) {
                integrator.moveCameraRelative(pose, input, 1.0 / fps, 4.0, 10.0);
            }
            double distance = Math.sqrt(pose.x() * pose.x() + pose.z() * pose.z());
            if (Double.isNaN(reference)) reference = distance;
            require(Math.abs(distance - reference) < 1.0E-8, "LOOK_AT movement depends on FPS: " + fps);
        }
    }

    private static boolean close(double a, double b) { return Math.abs(a - b) < 1.0E-5; }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
