package com.cukkoo.freecamera.input;

import com.cukkoo.freecamera.api.CameraMode;

public final class CameraModeKeyPriority {
    private CameraModeKeyPriority() {
    }

    public static CameraMode resolve(boolean freePressed, boolean orbitPressed, boolean tripodPressed) {
        if (freePressed) {
            return CameraMode.FREE_CAMERA;
        }
        if (orbitPressed) {
            return CameraMode.ORBIT;
        }
        return tripodPressed ? CameraMode.TRIPOD : null;
    }
}
