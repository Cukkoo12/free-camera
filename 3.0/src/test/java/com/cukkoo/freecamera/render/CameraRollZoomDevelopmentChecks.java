package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import com.cukkoo.freecamera.roll.CameraOrientationBridge;
import com.cukkoo.freecamera.roll.CameraRollController;
import com.cukkoo.freecamera.roll.CameraRollState;
import com.cukkoo.freecamera.zoom.CameraProjectionBridge;
import com.cukkoo.freecamera.zoom.CameraZoomController;
import com.cukkoo.freecamera.zoom.CameraZoomState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

public final class CameraRollZoomDevelopmentChecks {
    private static final int[] FRAME_RATES = {10, 20, 30, 60, 144, 240};
    private static final double TOLERANCE = 1.0E-8;

    private CameraRollZoomDevelopmentChecks() {
    }

    public static void main(String[] args) {
        compareRollAcrossFrameRates(CinematicMotionProfile.RESPONSIVE);
        compareRollAcrossFrameRates(CinematicMotionProfile.CINEMATIC);
        compareZoomAcrossFrameRates(CinematicMotionProfile.RESPONSIVE);
        compareZoomAcrossFrameRates(CinematicMotionProfile.CINEMATIC);
        verifyUnlimitedNormalizedRoll();
        verifyFullRotationRendering();
        verifyReleaseAndCinematicHold();
        verifyWrapContinuity();
        verifyResetToZero();
        verifyDefaultRollRate();
        verifyProfileResponses();
        verifyZoomPressAndRelease();
        verifyProfileChangeDoesNotJump();
        verifyModeAndScreenPreservation();
        verifyCleanupRestoresVanillaValues();
        verifySharedWorldViewAndProjectionBridges();
        System.out.println("Camera roll and zoom checks passed at 10, 20, 30, 60, 144, and 240 FPS.");
    }

    private static void compareRollAcrossFrameRates(CinematicMotionProfile profile) {
        EffectResult reference = simulateRoll(240, profile);
        for (int fps : FRAME_RATES) {
            EffectResult result = simulateRoll(fps, profile);
            require(result.maxDifference(reference) <= TOLERANCE,
                    profile + " roll differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
    }

    private static EffectResult simulateRoll(int fps, CinematicMotionProfile profile) {
        CameraRollController controller = new CameraRollController();
        double elapsed = 1.0 / fps;
        advanceRoll(controller, profile, fps * 8, elapsed, 1);
        advanceRoll(controller, profile, fps * 3, elapsed, 0);
        return new EffectResult(controller.state().currentRoll(), controller.state().velocity());
    }

    private static void compareZoomAcrossFrameRates(CinematicMotionProfile profile) {
        EffectResult reference = simulateZoom(240, profile);
        for (int fps : FRAME_RATES) {
            EffectResult result = simulateZoom(fps, profile);
            require(result.maxDifference(reference) <= TOLERANCE,
                    profile + " zoom differs at " + fps + " FPS: " + result.maxDifference(reference));
        }
    }

    private static EffectResult simulateZoom(int fps, CinematicMotionProfile profile) {
        CameraZoomController controller = new CameraZoomController();
        double elapsed = 1.0 / fps;
        controller.setHeldForDevelopmentCheck(true);
        advanceZoom(controller, profile, fps, elapsed);
        controller.setHeldForDevelopmentCheck(false);
        advanceZoom(controller, profile, fps * 2, elapsed);
        return new EffectResult(controller.state().currentMultiplier(), controller.state().velocity());
    }

    private static void verifyUnlimitedNormalizedRoll() {
        CameraRollController controller = new CameraRollController();
        advanceRoll(controller, CinematicMotionProfile.RESPONSIVE, 180, 1.0 / 60.0, 1);
        require(Math.abs(controller.state().currentRoll()) > 45.0,
                "continuous roll still appears clamped to 45 degrees");
        require(controller.state().currentRoll() >= -180.0
                        && controller.state().currentRoll() < 180.0,
                "current roll was not normalized to [-180, 180)");
        requireClose(CameraRollState.normalizeDegrees(360.0), 0.0, "normalize +360");
        requireClose(CameraRollState.normalizeDegrees(720.0), 0.0, "normalize +720");
        requireClose(CameraRollState.normalizeDegrees(-720.0), 0.0, "normalize -720");
    }

    private static void verifyFullRotationRendering() {
        CameraOrientationBridge bridge = new CameraOrientationBridge();
        Matrix4f zero = new Matrix4f();
        Matrix4f positiveFull = new Matrix4f();
        Matrix4f positiveDouble = new Matrix4f();
        Matrix4f negativeFull = new Matrix4f();
        bridge.applyWorldView(zero, 0.0F, 0.0F, 0.0);
        bridge.applyWorldView(positiveFull, 0.0F, 0.0F, 360.0);
        bridge.applyWorldView(positiveDouble, 0.0F, 0.0F, 720.0);
        bridge.applyWorldView(negativeFull, 0.0F, 0.0F, -360.0);
        requireMatrixClose(positiveFull, zero, "+360-degree render mismatch");
        requireMatrixClose(positiveDouble, zero, "+720-degree render mismatch");
        requireMatrixClose(negativeFull, zero, "-360-degree render mismatch");
    }

    private static void verifyReleaseAndCinematicHold() {
        for (CinematicMotionProfile profile : CinematicMotionProfile.values()) {
            CameraRollController controller = new CameraRollController();
            advanceRoll(controller, profile, 90, 1.0 / 60.0, 1);
            double releasedAngle = controller.state().currentRoll();
            require(Math.abs(releasedAngle) > 1.0, profile + " did not create roll");
            advanceRoll(controller, profile, 600, 1.0 / 60.0, 0);
            requireClose(controller.state().currentRoll(), releasedAngle,
                    profile + " auto-centered after release");
            requireClose(controller.state().velocity(), 0.0,
                    profile + " retained velocity after release");
        }
    }

    private static void verifyWrapContinuity() {
        double before = CameraRollState.normalizeDegrees(179.999);
        double after = CameraRollState.normalizeDegrees(180.001);
        requireClose(CameraRollState.normalizeDegrees(after - before), 0.002,
                "positive wrap reversed or snapped");
        double negativeBefore = CameraRollState.normalizeDegrees(-179.999);
        double negativeAfter = CameraRollState.normalizeDegrees(-180.001);
        requireClose(CameraRollState.normalizeDegrees(negativeAfter - negativeBefore), -0.002,
                "negative wrap reversed or snapped");

        CameraOrientationBridge bridge = new CameraOrientationBridge();
        Matrix4f beforeMatrix = new Matrix4f();
        Matrix4f afterMatrix = new Matrix4f();
        bridge.applyWorldView(beforeMatrix, 0.0F, 0.0F, before);
        bridge.applyWorldView(afterMatrix, 0.0F, 0.0F, after);
        require(maxMatrixDifference(beforeMatrix, afterMatrix) < 1.0E-4,
                "rendered view snapped at the +/-180-degree wrap");
    }

    private static void verifyResetToZero() {
        CameraRollController controller = new CameraRollController();
        advanceRoll(controller, CinematicMotionProfile.CINEMATIC, 120, 1.0 / 60.0, -1);
        require(Math.abs(controller.state().currentRoll()) > 1.0, "reset test never rolled");
        controller.resetToZero();
        requireClose(controller.state().currentRoll(), 0.0, "R roll reset");
        requireClose(controller.state().velocity(), 0.0, "R roll reset velocity");
    }

    private static void verifyProfileResponses() {
        CameraRollController responsive = new CameraRollController();
        CameraRollController cinematic = new CameraRollController();
        advanceRoll(responsive, CinematicMotionProfile.RESPONSIVE, 30, 1.0 / 60.0, 1);
        advanceRoll(cinematic, CinematicMotionProfile.CINEMATIC, 30, 1.0 / 60.0, 1);
        require(responsive.state().currentRoll() > cinematic.state().currentRoll(),
                "Cinematic roll was not slower than responsive roll");

        CameraZoomController responsiveZoom = new CameraZoomController();
        CameraZoomController cinematicZoom = new CameraZoomController();
        responsiveZoom.setHeldForDevelopmentCheck(true);
        cinematicZoom.setHeldForDevelopmentCheck(true);
        advanceZoom(responsiveZoom, CinematicMotionProfile.RESPONSIVE, 15, 1.0 / 60.0);
        advanceZoom(cinematicZoom, CinematicMotionProfile.CINEMATIC, 15, 1.0 / 60.0);
        require(responsiveZoom.state().currentMultiplier()
                        > cinematicZoom.state().currentMultiplier(),
                "Cinematic zoom was not slower than responsive zoom");
    }

    private static void verifyDefaultRollRate() {
        CameraRollController controller = new CameraRollController();
        double maximumVelocity = 0.0;
        for (int frame = 0; frame < 1200; frame++) {
            controller.advanceForDevelopmentCheck(
                    CinematicMotionProfile.RESPONSIVE,
                    1.0 / 240.0,
                    1
            );
            maximumVelocity = Math.max(maximumVelocity, Math.abs(controller.state().velocity()));
        }
        require(Math.abs(maximumVelocity - CameraRollState.DEFAULT_RATE) <= 0.05,
                "responsive roll rate differs from 60 degrees/second: " + maximumVelocity);
    }

    private static void verifyZoomPressAndRelease() {
        CameraZoomController controller = new CameraZoomController();
        controller.setHeldForDevelopmentCheck(true);
        advanceZoom(controller, CinematicMotionProfile.RESPONSIVE, 120, 1.0 / 60.0);
        requireClose(controller.state().currentMultiplier(),
                CameraZoomState.DEFAULT_ZOOM_MULTIPLIER, "zoom press target");
        controller.setHeldForDevelopmentCheck(false);
        advanceZoom(controller, CinematicMotionProfile.RESPONSIVE, 120, 1.0 / 60.0);
        requireClose(controller.state().currentMultiplier(),
                CameraZoomState.NORMAL_MULTIPLIER, "zoom release target");
    }

    private static void verifyProfileChangeDoesNotJump() {
        CameraRollController roll = new CameraRollController();
        CameraZoomController zoom = new CameraZoomController();
        zoom.setHeldForDevelopmentCheck(true);
        advanceRoll(roll, CinematicMotionProfile.RESPONSIVE, 20, 1.0 / 60.0, 1);
        advanceZoom(zoom, CinematicMotionProfile.RESPONSIVE, 20, 1.0 / 60.0);
        double rollBefore = roll.state().currentRoll();
        double zoomBefore = zoom.state().currentMultiplier();
        require(roll.state().currentRoll() == rollBefore
                        && zoom.state().currentMultiplier() == zoomBefore,
                "changing Cinematic profile changed roll or zoom without advancing time");
    }

    private static void verifyModeAndScreenPreservation() {
        CameraRollController roll = new CameraRollController();
        CameraZoomController zoom = new CameraZoomController();
        zoom.setHeldForDevelopmentCheck(true);
        advanceRoll(roll, CinematicMotionProfile.RESPONSIVE, 20, 1.0 / 60.0, 1);
        advanceZoom(zoom, CinematicMotionProfile.RESPONSIVE, 20, 1.0 / 60.0);
        double rollBefore = roll.state().currentRoll();
        double zoomBefore = zoom.state().currentMultiplier();
        roll.suspendInput();
        zoom.suspendInput();
        require(roll.state().currentRoll() == rollBefore,
                "screen or mode suspension changed roll");
        require(zoom.state().currentMultiplier() == zoomBefore,
                "screen or mode suspension changed zoom");
        requireClose(roll.state().velocity(), 0.0, "suspended roll velocity");
        requireClose(zoom.state().velocity(), 0.0, "suspended zoom velocity");
    }

    private static void verifyCleanupRestoresVanillaValues() {
        CameraRollController roll = new CameraRollController();
        CameraZoomController zoom = new CameraZoomController();
        zoom.setHeldForDevelopmentCheck(true);
        advanceRoll(roll, CinematicMotionProfile.RESPONSIVE, 30, 1.0 / 60.0, 1);
        advanceZoom(zoom, CinematicMotionProfile.RESPONSIVE, 30, 1.0 / 60.0);
        roll.clear();
        zoom.clear();
        requireClose(roll.state().currentRoll(), 0.0, "cleanup roll");
        requireClose(zoom.state().currentMultiplier(), 1.0, "cleanup zoom");
    }

    private static void verifySharedWorldViewAndProjectionBridges() {
        CameraRenderState renderState = new CameraRenderState();
        renderState.orientation.identity();
        renderState.viewRotationMatrix.identity();
        renderState.projectionMatrix.identity();
        Matrix4f hudProjection = new Matrix4f().perspective(
                (float) Math.toRadians(70.0), 16.0F / 9.0F, 0.05F, 100.0F
        );
        Matrix4f untouchedHudProjection = new Matrix4f(hudProjection);
        CameraOrientationBridge orientation = new CameraOrientationBridge();
        orientation.applyWorldView(renderState.viewRotationMatrix, 0.0F, 0.0F, 30.0);
        require(Math.abs(renderState.viewRotationMatrix.m01()) > 0.0,
                "rendered roll did not change the view matrix");
        requireClose(renderState.orientation.x(), 0.0, "semantic orientation x");
        requireClose(renderState.orientation.y(), 0.0, "semantic orientation y");
        requireClose(renderState.orientation.z(), 0.0, "semantic orientation z");
        requireClose(renderState.orientation.w(), 1.0, "semantic orientation w");
        requireMatrixClose(hudProjection, untouchedHudProjection, "HUD projection changed");
        orientation.updateScreenAxes(0.0F, 0.0F, 30.0);
        double rightLength = square(orientation.screenRightX())
                + square(orientation.screenRightY()) + square(orientation.screenRightZ());
        double upLength = square(orientation.screenUpX())
                + square(orientation.screenUpY()) + square(orientation.screenUpZ());
        requireClose(rightLength, 1.0, "rolled screen-right basis length");
        requireClose(upLength, 1.0, "rolled screen-up basis length");

        new CameraProjectionBridge().apply(renderState, 3.0);
        requireClose(renderState.projectionMatrix.m00(), 3.0, "zoom projection m00");
        requireClose(renderState.projectionMatrix.m11(), 3.0, "zoom projection m11");
        requireClose(renderState.projectionMatrix.m22(), 1.0, "zoom altered projection depth");

        Matrix4f finalViewProjection = new Matrix4f();
        CameraOrientationBridge.composeFinalViewProjection(
                renderState.projectionMatrix,
                renderState.viewRotationMatrix,
                finalViewProjection
        );
        Matrix4f expectedViewProjection = new Matrix4f(renderState.projectionMatrix)
                .mul(renderState.viewRotationMatrix);
        requireMatrixClose(finalViewProjection, expectedViewProjection,
                "frustum did not use final rolled and zoomed matrices");

        CameraRenderState nextFrame = new CameraRenderState();
        nextFrame.viewRotationMatrix.identity();
        nextFrame.projectionMatrix.identity();
        orientation.applyWorldView(nextFrame.viewRotationMatrix, 0.0F, 0.0F, 30.0);
        new CameraProjectionBridge().apply(nextFrame, 3.0);
        requireMatrixClose(nextFrame.viewRotationMatrix, renderState.viewRotationMatrix,
                "roll accumulated between fresh render states");
        requireMatrixClose(nextFrame.projectionMatrix, renderState.projectionMatrix,
                "zoom accumulated between fresh render states");

        CameraRenderState inactiveState = new CameraRenderState();
        inactiveState.viewRotationMatrix.identity();
        inactiveState.projectionMatrix.identity();
        Matrix4f vanillaView = new Matrix4f(inactiveState.viewRotationMatrix);
        Matrix4f vanillaProjection = new Matrix4f(inactiveState.projectionMatrix);
        requireMatrixClose(inactiveState.viewRotationMatrix, vanillaView,
                "inactive view was not vanilla");
        requireMatrixClose(inactiveState.projectionMatrix, vanillaProjection,
                "inactive projection was not vanilla");

        Matrix4f vanillaCanonicalView = new Matrix4f().rotationY(0.4F).rotateX(-0.2F);
        Matrix4f vanillaCanonicalProjection = new Matrix4f().perspective(
                (float) Math.toRadians(80.0), 16.0F / 9.0F, 0.05F, 512.0F
        );
        CanonicalWorldView canonical = new CanonicalWorldView();
        canonical.prepare(
                vanillaCanonicalView,
                vanillaCanonicalProjection,
                20.0F,
                -10.0F,
                30.0,
                3.0,
                orientation,
                new CameraProjectionBridge()
        );
        Matrix4f terrainSource = (Matrix4f) canonical.selectViewOrVanilla(vanillaCanonicalView);
        Matrix4f worldPassVanilla = new Matrix4f();
        Matrix4f worldPassSource = (Matrix4f) canonical.selectViewOrVanilla(worldPassVanilla);
        require(terrainSource == worldPassSource,
                "terrain and world passes did not receive the same canonical view object");
        Matrix4f firstCanonicalFrame = new Matrix4f(canonical.view());
        canonical.prepare(
                vanillaCanonicalView,
                vanillaCanonicalProjection,
                20.0F,
                -10.0F,
                30.0,
                3.0,
                orientation,
                new CameraProjectionBridge()
        );
        requireMatrixClose((Matrix4f) canonical.view(), firstCanonicalFrame,
                "canonical world view accumulated between frames");
        canonical.clear();
        Matrix4f inactiveVanilla = new Matrix4f().translation(1.0F, 2.0F, 3.0F);
        require(canonical.selectViewOrVanilla(inactiveVanilla) == inactiveVanilla,
                "inactive canonical selector replaced the vanilla view");
    }

    private static void advanceRoll(
            CameraRollController controller,
            CinematicMotionProfile profile,
            int frames,
            double elapsed,
            int direction
    ) {
        for (int frame = 0; frame < frames; frame++) {
            controller.advanceForDevelopmentCheck(profile, elapsed, direction);
        }
    }

    private static void advanceZoom(
            CameraZoomController controller,
            CinematicMotionProfile profile,
            int frames,
            double elapsed
    ) {
        for (int frame = 0; frame < frames; frame++) {
            controller.advanceForDevelopmentCheck(profile, elapsed);
        }
    }

    private static double square(double value) { return value * value; }

    private static void requireClose(double actual, double expected, String name) {
        require(Math.abs(actual - expected) <= TOLERANCE,
                name + ": expected " + expected + ", got " + actual);
    }

    private static void requireMatrixClose(Matrix4f actual, Matrix4f expected, String name) {
        float[] actualValues = new float[16];
        float[] expectedValues = new float[16];
        actual.get(actualValues);
        expected.get(expectedValues);
        for (int index = 0; index < actualValues.length; index++) {
            require(Math.abs(actualValues[index] - expectedValues[index]) <= TOLERANCE,
                    name + " at component " + index);
        }
    }

    private static double maxMatrixDifference(Matrix4f first, Matrix4f second) {
        float[] firstValues = new float[16];
        float[] secondValues = new float[16];
        first.get(firstValues);
        second.get(secondValues);
        double maximum = 0.0;
        for (int index = 0; index < firstValues.length; index++) {
            maximum = Math.max(maximum, Math.abs(firstValues[index] - secondValues[index]));
        }
        return maximum;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private record EffectResult(double value, double velocity) {
        double maxDifference(EffectResult other) {
            return Math.max(
                    Math.abs(CameraRollState.normalizeDegrees(value - other.value)),
                    Math.abs(velocity - other.velocity)
            );
        }
    }
}
