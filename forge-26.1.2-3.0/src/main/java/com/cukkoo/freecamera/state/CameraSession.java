package com.cukkoo.freecamera.state;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.lifecycle.ClientIdentitySnapshot;

public final class CameraSession {
    private CameraMode mode;
    private CameraPose desiredPose;
    private CameraPose renderedPose;
    private ClientIdentitySnapshot identity;

    public CameraSession(CameraMode mode, CameraPose pose, ClientIdentitySnapshot identity) {
        this.mode = mode;
        this.desiredPose = pose;
        this.renderedPose = new CameraPose(
                pose.x(), pose.y(), pose.z(), pose.yaw(), pose.pitch()
        );
        this.identity = identity;
    }

    public CameraMode mode() {
        return mode;
    }

    public void setMode(CameraMode mode) {
        this.mode = mode;
    }

    public CameraPose pose() {
        return desiredPose;
    }

    public CameraPose renderedPose() {
        return renderedPose;
    }

    public void prepareModeTransitionFromRendered() {
        desiredPose.copyFrom(renderedPose);
    }

    public ClientIdentitySnapshot identity() {
        return identity;
    }

    public void clear() {
        if (desiredPose != null) {
            desiredPose.clear();
        }
        if (renderedPose != null) {
            renderedPose.clear();
        }
        desiredPose = null;
        renderedPose = null;
        identity = null;
    }
}
