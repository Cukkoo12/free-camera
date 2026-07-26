package com.cukkoo.freecamera.roll;

import org.joml.Matrix4f;

public final class CameraOrientationBridge {
    private double screenRightX;
    private double screenRightY;
    private double screenRightZ;
    private double screenUpX;
    private double screenUpY;
    private double screenUpZ;

    public void applyWorldView(
            Matrix4f worldView,
            float yaw,
            float pitch,
            double rollDegrees
    ) {
        double normalizedRoll = CameraRollState.normalizeDegrees(rollDegrees);
        if (normalizedRoll == 0.0) {
            updateScreenAxes(yaw, pitch, 0.0);
            return;
        }
        float radians = (float) Math.toRadians(normalizedRoll);
        worldView.rotateLocalZ(-radians);
        updateScreenAxes(yaw, pitch, normalizedRoll);
    }

    public void updateScreenAxes(float yaw, float pitch, double rollDegrees) {
        double yawRadians = Math.toRadians(yaw);
        double pitchRadians = Math.toRadians(pitch);
        double rollRadians = Math.toRadians(CameraRollState.normalizeDegrees(rollDegrees));
        double rightX = -Math.cos(yawRadians);
        double rightY = 0.0;
        double rightZ = -Math.sin(yawRadians);
        double forwardX = -Math.sin(yawRadians) * Math.cos(pitchRadians);
        double forwardY = -Math.sin(pitchRadians);
        double forwardZ = Math.cos(yawRadians) * Math.cos(pitchRadians);
        double upX = rightY * forwardZ - rightZ * forwardY;
        double upY = rightZ * forwardX - rightX * forwardZ;
        double upZ = rightX * forwardY - rightY * forwardX;
        double cosRoll = Math.cos(rollRadians);
        double sinRoll = Math.sin(rollRadians);
        screenRightX = rightX * cosRoll + upX * sinRoll;
        screenRightY = rightY * cosRoll + upY * sinRoll;
        screenRightZ = rightZ * cosRoll + upZ * sinRoll;
        screenUpX = upX * cosRoll - rightX * sinRoll;
        screenUpY = upY * cosRoll - rightY * sinRoll;
        screenUpZ = upZ * cosRoll - rightZ * sinRoll;
    }

    public double screenRightX() { return screenRightX; }
    public double screenRightY() { return screenRightY; }
    public double screenRightZ() { return screenRightZ; }
    public double screenUpX() { return screenUpX; }
    public double screenUpY() { return screenUpY; }
    public double screenUpZ() { return screenUpZ; }
}
