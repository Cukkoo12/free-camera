package com.cukkoo.freecamera.state;

import com.cukkoo.freecamera.api.CameraMode;

public final class CameraModeTransition {
    public enum Action {
        ACTIVATE,
        SWITCH,
        DEACTIVATE
    }

    private CameraModeTransition() {
    }

    public static Action resolve(CameraMode activeMode, CameraMode requestedMode) {
        if (requestedMode == null) {
            throw new IllegalArgumentException("requestedMode");
        }
        if (activeMode == null) {
            return Action.ACTIVATE;
        }
        return activeMode == requestedMode ? Action.DEACTIVATE : Action.SWITCH;
    }
}
