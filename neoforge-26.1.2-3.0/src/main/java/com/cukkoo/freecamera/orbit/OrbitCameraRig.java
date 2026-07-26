package com.cukkoo.freecamera.orbit;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

public final class OrbitCameraRig {
    private static final double MIN_CONVERSION_DISTANCE_SQUARED = 1.0E-8;

    private final OrbitCameraState state = new OrbitCameraState();
    private final OrbitMotionIntegrator motionIntegrator = new OrbitMotionIntegrator();
    private double configuredRadius=OrbitCameraState.DEFAULT_RADIUS,sensitivity=1;
    public void configure(double radius,double sensitivity){configuredRadius=OrbitCameraState.clampRadius(radius);this.sensitivity=Math.clamp(sensitivity,.1,5);}

    public OrbitCameraState state() {
        return state;
    }

    OrbitMotionIntegrator motionIntegratorForDevelopmentCheck() {
        return motionIntegrator;
    }

    public void enterFromPose(CameraPose pose, LocalPlayer player) {
        enterFromPose(
                pose,
                player.getX(),
                player.getEyeY(),
                player.getZ(),
                player.getYRot()
        );
    }

    void enterFromPose(
            CameraPose pose,
            double anchorX,
            double anchorY,
            double anchorZ,
            float fallbackPlayerYaw
    ) {
        double offsetX = pose.x() - anchorX;
        double offsetY = pose.y() - anchorY;
        double offsetZ = pose.z() - anchorZ;
        double distanceSquared = offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ;
        if (Double.isFinite(distanceSquared) && distanceSquared > MIN_CONVERSION_DISTANCE_SQUARED) {
            double distance = Math.sqrt(distanceSquared);
            float yaw = (float) Math.toDegrees(Math.atan2(-offsetX, offsetZ));
            float pitch = (float) Math.toDegrees(Math.asin(Math.clamp(offsetY / distance, -1.0, 1.0)));
            state.initialize(yaw, pitch, distance, 0.0);
        } else {
            state.initialize(fallbackPlayerYaw + 180.0F, 0.0F, configuredRadius, 0.0);
        }
        motionIntegrator.clearTransientState(state);
        applyPose(pose, anchorX, anchorY, anchorZ);
    }

    public void advance(
            Minecraft client,
            CameraPose pose,
            CameraInputSnapshot input,
            LocalPlayer player,
            CinematicMotionProfile profile
    ) {
        motionIntegrator.advance(client, state, input, profile);
        applyPose(pose, player.getX(), player.getEyeY(), player.getZ());
    }

    public void rotateFromMouse(CameraPose pose, LocalPlayer player, double yawInput, double pitchInput) {
        state.rotate((float) (yawInput * 0.15F*sensitivity), (float) (pitchInput * 0.15F*sensitivity));
        applyPose(pose, player.getX(), player.getEyeY(), player.getZ());
    }

    public void applySmoothedRotation(
            CameraPose pose,
            LocalPlayer player,
            float yaw,
            float pitch
    ) {
        state.setRotation(yaw, pitch);
        applyPose(pose, player.getX(), player.getEyeY(), player.getZ());
    }

    public boolean requestRadiusStep(double verticalScroll) {
        return state.requestRadiusStep((int) Math.signum(verticalScroll));
    }

    public double requestedRadiusForMessage() {
        return state.hasRadiusTarget() ? state.requestedRadius() : state.radius();
    }

    public void resetToDefault(CameraPose pose, LocalPlayer player) {
        state.initialize(player.getYRot() + 180.0F, 0.0F, configuredRadius, 0.0);
        motionIntegrator.clearTransientState(state);
        applyPose(pose, player.getX(), player.getEyeY(), player.getZ());
    }

    public void leaveToFree() {
        clear();
    }

    public void suspendMotion() {
        motionIntegrator.clearTransientState(state);
    }

    public void clear() {
        motionIntegrator.clearTransientState(state);
        state.clear();
    }

    void applyPose(CameraPose pose, double anchorX, double anchorY, double anchorZ) {
        double yawRadians = Math.toRadians(state.yaw());
        double pitchRadians = Math.toRadians(state.pitch());
        double horizontalRadius = Math.cos(pitchRadians) * state.radius();
        double offsetX = -Math.sin(yawRadians) * horizontalRadius;
        double offsetY = Math.sin(pitchRadians) * state.radius();
        double offsetZ = Math.cos(yawRadians) * horizontalRadius;
        pose.set(
                anchorX + offsetX,
                anchorY + state.anchorOffsetY() + offsetY,
                anchorZ + offsetZ,
                Mth.wrapDegrees(state.yaw() + 180.0F),
                state.pitch()
        );
    }
}
