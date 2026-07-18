package com.cukkoo.freecamera.tripod;

import com.cukkoo.freecamera.api.CameraPose;

public final class TripodCameraState {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;
    private boolean captured;

    public void capture(CameraPose pose) {
        x = pose.x();
        y = pose.y();
        z = pose.z();
        yaw = pose.yaw();
        pitch = pose.pitch();
        captured = true;
    }

    public void applyTo(CameraPose pose) {
        if (captured) {
            pose.set(x, y, z, yaw, pitch);
        }
    }

    public boolean isCaptured() {
        return captured;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    public void clear() {
        x = 0.0;
        y = 0.0;
        z = 0.0;
        yaw = 0.0F;
        pitch = 0.0F;
        captured = false;
    }
}
