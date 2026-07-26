package com.cukkoo.freecamera.collision;

import com.cukkoo.freecamera.api.CameraMode;

public enum CameraCollisionMode {
    OFF,
    SOFT,
    VANILLA;

    public static CameraCollisionMode defaultFor(CameraMode cameraMode) {
        return switch (cameraMode) {
            case FREE_CAMERA, TRIPOD -> OFF;
            case ORBIT, FOLLOW -> SOFT;
        };
    }
}
