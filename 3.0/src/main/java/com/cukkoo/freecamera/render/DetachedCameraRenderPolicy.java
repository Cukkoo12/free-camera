package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.api.CameraMode;

public final class DetachedCameraRenderPolicy {
    private DetachedCameraRenderPolicy() {
    }

    public static boolean hideHand(CameraMode activeMode) {
        return activeMode != null;
    }

    public static boolean hideCrosshair(CameraMode activeMode) {
        return activeMode != null;
    }
}
