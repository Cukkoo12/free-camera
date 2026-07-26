package com.cukkoo.freecamera.cinematic;

public enum CinematicMotionProfile {
    RESPONSIVE(10.0, 14.0, 10.0, 12.0, 18.0, 3.624, 12.0),
    CINEMATIC(3.8, 3.0, 3.8, 5.0, 7.0, 2.2, 5.0);

    private final double accelerationResponse;
    private final double decelerationResponse;
    private final double reversalResponse;
    private final double radiusTargetResponse;
    private final double rotationFrequency;
    private final double rollFrequency;
    private final double zoomFrequency;

    CinematicMotionProfile(
            double accelerationResponse,
            double decelerationResponse,
            double reversalResponse,
            double radiusTargetResponse,
            double rotationFrequency,
            double rollFrequency,
            double zoomFrequency
    ) {
        this.accelerationResponse = accelerationResponse;
        this.decelerationResponse = decelerationResponse;
        this.reversalResponse = reversalResponse;
        this.radiusTargetResponse = radiusTargetResponse;
        this.rotationFrequency = rotationFrequency;
        this.rollFrequency = rollFrequency;
        this.zoomFrequency = zoomFrequency;
    }

    public double accelerationResponse() {
        return accelerationResponse;
    }

    public double decelerationResponse() {
        return decelerationResponse;
    }

    public double reversalResponse() {
        return reversalResponse;
    }

    public double radiusTargetResponse() {
        return radiusTargetResponse;
    }

    public double rotationFrequency() {
        return rotationFrequency;
    }

    public double rollFrequency() {
        return rollFrequency;
    }

    public double zoomFrequency() {
        return zoomFrequency;
    }
}
