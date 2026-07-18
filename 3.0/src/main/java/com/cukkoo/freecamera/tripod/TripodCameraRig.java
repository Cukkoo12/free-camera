package com.cukkoo.freecamera.tripod;

import com.cukkoo.freecamera.api.CameraPose;

public final class TripodCameraRig {
    private final TripodCameraState state = new TripodCameraState();

    public TripodCameraState state() {
        return state;
    }

    public void enterFromPose(CameraPose pose) {
        state.capture(pose);
        state.applyTo(pose);
    }

    public void applyPose(CameraPose pose) {
        state.applyTo(pose);
    }

    public void leave() {
        state.clear();
    }

    public void clear() {
        state.clear();
    }
}
