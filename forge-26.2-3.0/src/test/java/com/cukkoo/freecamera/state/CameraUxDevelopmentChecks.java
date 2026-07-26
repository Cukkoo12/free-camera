package com.cukkoo.freecamera.state;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.input.CameraInputPolicy;
import com.cukkoo.freecamera.input.CameraModeKeyPriority;
import com.cukkoo.freecamera.render.DetachedCameraRenderPolicy;

public final class CameraUxDevelopmentChecks {
    private CameraUxDevelopmentChecks() {
    }

    public static void main(String[] args) {
        requireTransition(null, CameraMode.FREE_CAMERA,
                CameraModeTransition.Action.ACTIVATE, CameraMode.FREE_CAMERA,
                "inactive + direct Free");
        requireTransition(null, CameraMode.ORBIT,
                CameraModeTransition.Action.ACTIVATE, CameraMode.ORBIT,
                "inactive + direct Orbit");
        requireTransition(CameraMode.FREE_CAMERA, CameraMode.ORBIT,
                CameraModeTransition.Action.SWITCH, CameraMode.ORBIT,
                "active Free + direct Orbit");
        requireTransition(CameraMode.ORBIT, CameraMode.FREE_CAMERA,
                CameraModeTransition.Action.SWITCH, CameraMode.FREE_CAMERA,
                "active Orbit + direct Free");
        requireTransition(CameraMode.ORBIT, CameraMode.ORBIT,
                CameraModeTransition.Action.DEACTIVATE, null,
                "active Orbit + direct Orbit");
        requireTransition(null, CameraMode.TRIPOD,
                CameraModeTransition.Action.ACTIVATE, CameraMode.TRIPOD,
                "inactive + direct Tripod");
        requireTransition(CameraMode.FREE_CAMERA, CameraMode.TRIPOD,
                CameraModeTransition.Action.SWITCH, CameraMode.TRIPOD,
                "active Free + F8");
        requireTransition(CameraMode.ORBIT, CameraMode.TRIPOD,
                CameraModeTransition.Action.SWITCH, CameraMode.TRIPOD,
                "active Orbit + F8");
        requireTransition(CameraMode.TRIPOD, CameraMode.TRIPOD,
                CameraModeTransition.Action.DEACTIVATE, null,
                "active Tripod + F8");
        requireTransition(CameraMode.TRIPOD, CameraMode.FREE_CAMERA,
                CameraModeTransition.Action.SWITCH, CameraMode.FREE_CAMERA,
                "active Tripod + F6");
        requireTransition(CameraMode.TRIPOD, CameraMode.ORBIT,
                CameraModeTransition.Action.SWITCH, CameraMode.ORBIT,
                "active Tripod + F7");
        verifyInputPolicy();
        verifyModeKeyPriority();
        verifyDetachedRenderPolicy();
        System.out.println("Direct camera-mode transition checks passed.");
    }

    private static void verifyInputPolicy() {
        require(CameraInputPolicy.cameraReceivesMovementAndMouse(CameraMode.FREE_CAMERA),
                "Free Camera must receive movement and mouse input");
        require(CameraInputPolicy.cameraReceivesMovementAndMouse(CameraMode.ORBIT),
                "Orbit must receive movement and mouse input");
        require(!CameraInputPolicy.cameraReceivesMovementAndMouse(CameraMode.TRIPOD),
                "Tripod must not receive movement or mouse input");
        require(CameraInputPolicy.playerMovementIsSuppressed(CameraMode.FREE_CAMERA),
                "Free Camera must suppress real-player movement input");
        require(CameraInputPolicy.playerMovementIsSuppressed(CameraMode.ORBIT),
                "Orbit must suppress real-player movement input");
        require(!CameraInputPolicy.playerMovementIsSuppressed(CameraMode.TRIPOD),
                "Tripod must leave real-player movement input vanilla");
        require(!CameraInputPolicy.cameraReceivesMovementAndMouse(null),
                "Inactive camera must leave mouse input vanilla");
        require(!CameraInputPolicy.playerMovementIsSuppressed(null),
                "Inactive camera must leave movement input vanilla");
    }

    private static void verifyModeKeyPriority() {
        require(CameraModeKeyPriority.resolve(true, true, true) == CameraMode.FREE_CAMERA,
                "Simultaneous F6/F7/F8 must resolve only to F6");
        require(CameraModeKeyPriority.resolve(false, true, true) == CameraMode.ORBIT,
                "Simultaneous F7/F8 must resolve only to F7");
        require(CameraModeKeyPriority.resolve(false, false, true) == CameraMode.TRIPOD,
                "F8 must resolve to Tripod");
        require(CameraModeKeyPriority.resolve(false, false, false) == null,
                "No key must produce no transition");
    }

    private static void verifyDetachedRenderPolicy() {
        for (CameraMode mode : CameraMode.values()) {
            require(DetachedCameraRenderPolicy.hideHand(mode),
                    "hand must be hidden in " + mode);
            require(DetachedCameraRenderPolicy.hideCrosshair(mode),
                    "crosshair must be hidden in " + mode);
        }
        require(!DetachedCameraRenderPolicy.hideHand(null),
                "hand must remain vanilla while inactive");
        require(!DetachedCameraRenderPolicy.hideCrosshair(null),
                "crosshair must remain vanilla while inactive");
    }

    private static void requireTransition(
            CameraMode activeMode,
            CameraMode requestedMode,
            CameraModeTransition.Action expectedAction,
            CameraMode expectedResult,
            String scenario
    ) {
        CameraModeTransition.Action action = CameraModeTransition.resolve(activeMode, requestedMode);
        if (action != expectedAction) {
            throw new AssertionError(scenario + " resolved to " + action + " instead of " + expectedAction);
        }
        CameraMode result = switch (action) {
            case ACTIVATE, SWITCH -> requestedMode;
            case DEACTIVATE -> null;
        };
        if (result != expectedResult) {
            throw new AssertionError(scenario + " produced " + result + " instead of " + expectedResult);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
