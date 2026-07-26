package com.cukkoo.freecamera.input;

import com.cukkoo.freecamera.api.CameraMode;

public final class CameraInputPolicy {
    private CameraInputPolicy() {
    }

    public static boolean cameraReceivesMovementAndMouse(CameraMode mode) {
        return mode == CameraMode.FREE_CAMERA || mode == CameraMode.ORBIT || mode == CameraMode.FOLLOW;
    }

    public static boolean playerMovementIsSuppressed(CameraMode mode) {
        return cameraReceivesMovementAndMouse(mode);
    }
}
