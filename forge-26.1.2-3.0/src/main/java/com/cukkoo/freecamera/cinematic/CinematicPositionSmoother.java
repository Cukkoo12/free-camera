package com.cukkoo.freecamera.cinematic;

public final class CinematicPositionSmoother {
    private static final double SETTLE_EPSILON = 1.0E-8;

    private double velocityX;
    private double velocityY;
    private double velocityZ;
    private double targetX;
    private double targetY;
    private double targetZ;
    private double displacementX;
    private double displacementY;
    private double displacementZ;
    private double responsiveAcceleration=10,responsiveDeceleration=14,cinematicAcceleration=3.8,cinematicDeceleration=3;
    public void configure(double acceleration,double deceleration,double cinematicAcceleration,double cinematicDeceleration){this.responsiveAcceleration=positive(acceleration,10);this.responsiveDeceleration=positive(deceleration,14);this.cinematicAcceleration=positive(cinematicAcceleration,3.8);this.cinematicDeceleration=positive(cinematicDeceleration,3);}

    public void setTargetVelocity(double x, double y, double z) {
        targetX = finiteOrZero(x);
        targetY = finiteOrZero(y);
        targetZ = finiteOrZero(z);
    }

    public void integrate(CinematicMotionProfile profile, double elapsedSeconds) {
        double response = selectResponse(profile);
        double decay = Math.exp(-response * elapsedSeconds);
        double responseIntegral = (1.0 - decay) / response;
        displacementX = targetX * elapsedSeconds + (velocityX - targetX) * responseIntegral;
        displacementY = targetY * elapsedSeconds + (velocityY - targetY) * responseIntegral;
        displacementZ = targetZ * elapsedSeconds + (velocityZ - targetZ) * responseIntegral;
        velocityX = targetX + (velocityX - targetX) * decay;
        velocityY = targetY + (velocityY - targetY) * decay;
        velocityZ = targetZ + (velocityZ - targetZ) * decay;
        settleIfIdle();
    }

    public double displacementX() {
        return displacementX;
    }

    public double displacementY() {
        return displacementY;
    }

    public double displacementZ() {
        return displacementZ;
    }

    public double velocityX() {
        return velocityX;
    }

    public double velocityY() {
        return velocityY;
    }

    public double velocityZ() {
        return velocityZ;
    }

    public void clear() {
        velocityX = 0.0;
        velocityY = 0.0;
        velocityZ = 0.0;
        targetX = 0.0;
        targetY = 0.0;
        targetZ = 0.0;
        displacementX = 0.0;
        displacementY = 0.0;
        displacementZ = 0.0;
    }

    private double selectResponse(CinematicMotionProfile profile) {
        double targetLengthSquared = targetX * targetX + targetY * targetY + targetZ * targetZ;
        if (targetLengthSquared <= SETTLE_EPSILON) {
            return profile==CinematicMotionProfile.CINEMATIC?cinematicDeceleration:responsiveDeceleration;
        }
        double directionDot = velocityX * targetX + velocityY * targetY + velocityZ * targetZ;
        return directionDot < 0.0
                ? (profile==CinematicMotionProfile.CINEMATIC?cinematicAcceleration:responsiveAcceleration)
                : (profile==CinematicMotionProfile.CINEMATIC?cinematicAcceleration:responsiveAcceleration);
    }

    private void settleIfIdle() {
        if (targetX == 0.0 && targetY == 0.0 && targetZ == 0.0
                && velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ
                <= SETTLE_EPSILON * SETTLE_EPSILON) {
            velocityX = 0.0;
            velocityY = 0.0;
            velocityZ = 0.0;
        }
    }

    private static double finiteOrZero(double value) {
        return Double.isFinite(value) ? value : 0.0;
    }
    private static double positive(double v,double fallback){return Double.isFinite(v)&&v>0?v:fallback;}
}
