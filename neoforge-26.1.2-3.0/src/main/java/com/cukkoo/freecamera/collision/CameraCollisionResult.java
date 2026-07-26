package com.cukkoo.freecamera.collision;

final class CameraCollisionResult {
    double x;
    double y;
    double z;
    boolean obstructed;
    boolean unloadedBoundary;

    CameraCollisionResult set(
            double x,
            double y,
            double z,
            boolean obstructed,
            boolean unloadedBoundary
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.obstructed = obstructed;
        this.unloadedBoundary = unloadedBoundary;
        return this;
    }
}
