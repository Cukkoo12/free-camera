package com.cukkoo.freecamera.radial;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.cinematic.CinematicMotionState;
import com.cukkoo.freecamera.orbit.OrbitCameraRig;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.tripod.TripodCameraRig;

public final class CameraRadialMenuDevelopmentChecks {
    private CameraRadialMenuDevelopmentChecks() {
    }

    public static void main(String[] args) {
        verifyShortPress();
        verifyHoldDoesNotShortPress();
        verifyLongReleaseCannotBecomeShortPress();
        verifyCenterReleaseCancels();
        verifyAllModeSelections();
        verifySelectionActionsAndPosePreservation();
        verifyCancelPathsAndQueuedRelease();
        verifyLastUsedMode();
        verifyCleanup();
        System.out.println("Camera radial-menu timing, geometry, transition, and cleanup checks passed.");
    }

    private static void verifyShortPress() {
        Fixture fixture = new Fixture();
        require(fixture.primary.updateKeyState(true, 1_000_000L)
                        == CameraPrimaryKeyController.KeyDecision.NONE,
                "F6 press triggered before release");
        require(fixture.primary.updateKeyState(false, 101_000_000L)
                        == CameraPrimaryKeyController.KeyDecision.SHORT_PRESS,
                "F6 short release did not trigger short press");
    }

    private static void verifyHoldDoesNotShortPress() {
        Fixture fixture = new Fixture();
        fixture.primary.updateKeyState(true, 1L);
        require(fixture.primary.updateKeyState(true,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS)
                        == CameraPrimaryKeyController.KeyDecision.NONE,
                "wheel opened before the full hold threshold");
        require(fixture.primary.updateKeyState(true,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS + 1L)
                        == CameraPrimaryKeyController.KeyDecision.OPENED,
                "wheel did not open at 0.30 seconds");
        require(fixture.primary.isInputSuppressed(), "open wheel did not suppress input");
        require(fixture.primary.updateKeyState(false,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS + 2L)
                        == CameraPrimaryKeyController.KeyDecision.CANCEL,
                "center release did not cancel");
    }

    private static void verifyCenterReleaseCancels() {
        Fixture fixture = new Fixture();
        fixture.radial.open();
        fixture.radial.accumulateMouse(2.0, 3.0, 1.0);
        require(fixture.radial.state().highlightedEntry() == null,
                "dead-zone movement selected a mode");
        require(fixture.radial.confirmAndClose() == null, "dead zone did not cancel");
    }

    private static void verifyLongReleaseCannotBecomeShortPress() {
        Fixture fixture = new Fixture();
        fixture.primary.updateKeyState(true, 1L);
        require(fixture.primary.updateKeyState(
                        false,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS + 1L
                ) == CameraPrimaryKeyController.KeyDecision.CANCEL,
                "long F6 release without an intermediate tick became a short press");
    }

    private static void verifyAllModeSelections() {
        requireSelection(0.0, -80.0, CameraMode.FREE_CAMERA, "Free");
        requireSelection(80.0, 0.0, CameraMode.ORBIT, "Orbit");
        requireSelection(0.0, 80.0, CameraMode.TRIPOD, "Tripod");
        requireSelection(-80.0, 0.0, CameraMode.FOLLOW, "Follow");
    }

    private static void verifySelectionActionsAndPosePreservation() {
        require(CameraPrimaryKeyController.resolveSelectionAction(null, CameraMode.FREE_CAMERA)
                        == CameraPrimaryKeyController.SelectionAction.ACTIVATE,
                "inactive Free selection did not activate");
        require(CameraPrimaryKeyController.resolveSelectionAction(null, CameraMode.ORBIT)
                        == CameraPrimaryKeyController.SelectionAction.ACTIVATE,
                "inactive Orbit selection did not activate");
        require(CameraPrimaryKeyController.resolveSelectionAction(null, CameraMode.TRIPOD)
                        == CameraPrimaryKeyController.SelectionAction.ACTIVATE,
                "inactive Tripod selection did not activate");
        require(CameraPrimaryKeyController.resolveSelectionAction(
                        CameraMode.FREE_CAMERA, CameraMode.ORBIT)
                        == CameraPrimaryKeyController.SelectionAction.SWITCH,
                "Free to Orbit did not switch");
        require(CameraPrimaryKeyController.resolveSelectionAction(
                        CameraMode.ORBIT, CameraMode.ORBIT)
                        == CameraPrimaryKeyController.SelectionAction.KEEP_CURRENT,
                "current-mode radial selection was not a no-op");

        CameraPose pose = new CameraPose(13.0, 72.0, -8.0, 42.0F, -17.0F);
        double x = pose.x();
        double y = pose.y();
        double z = pose.z();
        float yaw = pose.yaw();
        float pitch = pose.pitch();
        CameraPrimaryKeyController.resolveSelectionAction(CameraMode.FREE_CAMERA, CameraMode.ORBIT);
        require(pose.x() == x && pose.y() == y && pose.z() == z
                        && pose.yaw() == yaw && pose.pitch() == pitch,
                "radial transition resolution changed the rendered pose");
    }

    private static void verifyCancelPathsAndQueuedRelease() {
        Fixture fixture = new Fixture();
        fixture.primary.updateKeyState(true, 1L);
        fixture.primary.updateKeyState(true, CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS + 1L);
        fixture.primary.cancelUntilPrimaryRelease();
        require(!fixture.radial.state().isOpen(), "Escape/right-click did not close wheel");
        require(fixture.primary.isInputSuppressed(), "cancel did not quarantine input");
        require(fixture.primary.updateKeyState(true,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS * 2L)
                        == CameraPrimaryKeyController.KeyDecision.NONE,
                "cancelled held F6 reopened the wheel");
        require(fixture.primary.updateKeyState(false,
                        CameraPrimaryKeyController.HOLD_THRESHOLD_NANOS * 2L + 1L)
                        == CameraPrimaryKeyController.KeyDecision.NONE,
                "cancelled F6 release triggered a queued action");
    }

    private static void verifyLastUsedMode() {
        Fixture fixture = new Fixture();
        require(fixture.primary.lastUsedMode() == CameraMode.FREE_CAMERA,
                "default last-used mode was not Free");
        fixture.primary.rememberSuccessfulMode(CameraMode.TRIPOD);
        require(fixture.primary.lastUsedMode() == CameraMode.TRIPOD,
                "successful selection was not remembered");
    }

    private static void verifyCleanup() {
        Fixture fixture = new Fixture();
        fixture.radial.open();
        fixture.radial.accumulateMouse(90.0, 0.0, 1.0);
        fixture.primary.clear();
        require(!fixture.radial.state().isOpen(), "cleanup retained open wheel");
        require(fixture.radial.state().highlightedEntry() == null,
                "cleanup retained highlighted entry");
        require(fixture.radial.state().selectionX() == 0.0
                        && fixture.radial.state().selectionY() == 0.0,
                "cleanup retained mouse accumulation");
        require(!fixture.primary.isInputSuppressed(),
                "cleanup retained temporary input suppression");
    }

    private static void requireSelection(
            double x,
            double y,
            CameraMode expected,
            String name
    ) {
        CameraRadialMenuController radial = new CameraRadialMenuController(
                new CameraRadialMenuState()
        );
        radial.open();
        radial.accumulateMouse(x, y, 1.0);
        CameraRadialEntry selection = radial.confirmAndClose();
        require(selection != null && selection.mode() == expected,
                name + " radial direction selected " + selection);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Fixture {
        final CameraRadialMenuController radial = new CameraRadialMenuController(
                new CameraRadialMenuState()
        );
        final CameraPrimaryKeyController primary = new CameraPrimaryKeyController(
                new CameraStateMachine(
                        new OrbitCameraRig(),
                        new TripodCameraRig(),
                        new CinematicMotionState()
                ),
                radial
        );
    }
}
