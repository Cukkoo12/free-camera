package com.cukkoo.freecamera.input;

public final class CameraInputSnapshot {
    private double forward;
    private double strafe;
    private double vertical;

    public double forward() {
        return forward;
    }

    public double strafe() {
        return strafe;
    }

    public double vertical() {
        return vertical;
    }

    public boolean hasMovement() {
        return forward != 0.0 || strafe != 0.0 || vertical != 0.0;
    }

    public void set(double forward, double strafe, double vertical) {
        this.forward = forward;
        this.strafe = strafe;
        this.vertical = vertical;
    }

    public void clear() {
        forward = 0.0;
        strafe = 0.0;
        vertical = 0.0;
    }
}
