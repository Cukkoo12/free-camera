package com.cukkoo.freecamera.motion;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import com.cukkoo.freecamera.cinematic.CinematicPositionSmoother;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public final class CameraMotionIntegrator {
    static final double MAX_SUBSTEP_SECONDS = 0.05;
    static final double MAX_ACCEPTED_ELAPSED_SECONDS = 0.25;
    static final int MAX_SUBSTEP_COUNT = 5;
    private SpeedPreset speedPreset = SpeedPreset.NORMAL;
    private final CinematicPositionSmoother positionSmoother = new CinematicPositionSmoother();
    private long lastFrameNanos;
    private double customSpeed=4;private boolean customSelected;
    private double maximumAcceptedElapsed=MAX_ACCEPTED_ELAPSED_SECONDS;private int maximumSubsteps=MAX_SUBSTEP_COUNT;
    public void configure(String preset,double customSpeed,double acceleration,double deceleration,double cinematicAcceleration,double cinematicDeceleration,double maximumFrameTime,int movementSubsteps){
        customSelected="CUSTOM".equals(preset);try{if(!customSelected)speedPreset=SpeedPreset.valueOf(preset);}catch(IllegalArgumentException ignored){}this.customSpeed=Double.isFinite(customSpeed)&&customSpeed>0?customSpeed:4;positionSmoother.configure(acceleration,deceleration,cinematicAcceleration,cinematicDeceleration);
        maximumAcceptedElapsed=Double.isFinite(maximumFrameTime)?Math.clamp(maximumFrameTime,.05,.5):MAX_ACCEPTED_ELAPSED_SECONDS;maximumSubsteps=Math.clamp(movementSubsteps,1,20);
    }

    public void advance(
            Minecraft client,
            CameraPose pose,
            CameraInputSnapshot input,
            CinematicMotionProfile profile
    ) {
        if (client.screen != null || !client.isWindowActive()) {
            clearMotionState();
            return;
        }

        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }

        double elapsedSeconds = (now - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = now;
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearMotionState();
            return;
        }

        integrateAcceptedElapsed(pose, input, profile, elapsedSeconds);
    }

    void advanceForDevelopmentCheck(
            CameraPose pose,
            CameraInputSnapshot input,
            double elapsedSeconds
    ) {
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearMotionState();
            return;
        }
        integrateAcceptedElapsed(pose, input, CinematicMotionProfile.RESPONSIVE, elapsedSeconds);
    }

    public void advanceForDevelopmentCheck(
            CameraPose pose,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            double elapsedSeconds
    ) {
        if (!isAcceptedElapsedTime(elapsedSeconds)) {
            clearMotionState();
            return;
        }
        integrateAcceptedElapsed(pose, input, profile, elapsedSeconds);
    }

    double velocityXForDevelopmentCheck() {
        return positionSmoother.velocityX();
    }

    double velocityYForDevelopmentCheck() {
        return positionSmoother.velocityY();
    }

    double velocityZForDevelopmentCheck() {
        return positionSmoother.velocityZ();
    }

    private void integrateAcceptedElapsed(
            CameraPose pose,
            CameraInputSnapshot input,
            CinematicMotionProfile profile,
            double elapsedSeconds
    ) {
        int substepCount = Math.min(
                maximumSubsteps,
                Math.max(1, (int) Math.ceil(elapsedSeconds / MAX_SUBSTEP_SECONDS))
        );

        updateTargetVelocity(pose, input);
        double remainingSeconds = elapsedSeconds;
        for (int substep = 0; substep < substepCount; substep++) {
            double stepSeconds = Math.min(MAX_SUBSTEP_SECONDS, remainingSeconds);
            integrateSubstep(pose, profile, stepSeconds);
            remainingSeconds -= stepSeconds;
        }
    }

    private void integrateSubstep(
            CameraPose pose,
            CinematicMotionProfile profile,
            double stepSeconds
    ) {
        positionSmoother.integrate(profile, stepSeconds);
        pose.translate(
                positionSmoother.displacementX(),
                positionSmoother.displacementY(),
                positionSmoother.displacementZ()
        );
    }

    public boolean changeSpeedPreset(Minecraft client, double verticalScroll) {
        int direction = (int) Math.signum(verticalScroll);
        if (direction == 0) {
            return false;
        }
        SpeedPreset next = speedPreset.step(direction);
        if (next == speedPreset) {
            return false;
        }
        speedPreset = next;
        customSelected=false;
        client.gui.setOverlayMessage(Component.translatable(
                "message.free-camera.speed",
                Component.translatable(speedPreset.translationKey())
        ), false);
        return true;
    }

    public SpeedPreset speedPreset() {
        return speedPreset;
    }
    public double displayedSpeed(){return customSelected?customSpeed:speedPreset.blocksPerSecond();}

    public void clearMotionState() {
        positionSmoother.clear();
        lastFrameNanos = 0L;
    }

    private void updateTargetVelocity(CameraPose pose, CameraInputSnapshot input) {
        double yawRadians = Math.toRadians(pose.yaw());
        double pitchRadians = Math.toRadians(pose.pitch());
        double cosYaw = Math.cos(yawRadians);
        double sinYaw = Math.sin(yawRadians);
        double cosPitch = Math.cos(pitchRadians);
        double sinPitch = Math.sin(pitchRadians);

        double directionX = input.forward() * -sinYaw * cosPitch - input.strafe() * cosYaw;
        double directionY = input.forward() * -sinPitch + input.vertical();
        double directionZ = input.forward() * cosYaw * cosPitch - input.strafe() * sinYaw;
        double lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
        if (lengthSquared <= 0.0 || !Double.isFinite(lengthSquared)) {
            positionSmoother.setTargetVelocity(0.0, 0.0, 0.0);
            return;
        }

        double configured=customSelected?customSpeed:speedPreset.blocksPerSecond();
        double speedScale = configured / Math.sqrt(lengthSquared);
        positionSmoother.setTargetVelocity(
                directionX * speedScale,
                directionY * speedScale,
                directionZ * speedScale
        );
    }

    private boolean isAcceptedElapsedTime(double elapsedSeconds) {
        return Double.isFinite(elapsedSeconds)
                && elapsedSeconds > 0.0
                && elapsedSeconds <= maximumAcceptedElapsed;
    }
}
