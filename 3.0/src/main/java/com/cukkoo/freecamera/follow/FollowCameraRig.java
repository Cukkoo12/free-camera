package com.cukkoo.freecamera.follow;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

public final class FollowCameraRig {
    private static final double CONTROL_DISTANCE_RATE = 8.0;
    private static final double CONTROL_HEIGHT_RATE = 3.0;
    private static final double CONTROL_ORBIT_RATE = 65.0;
    private static final double TARGET_DISCONTINUITY_SQUARED = 1024.0;

    private final FollowCameraState state = new FollowCameraState();
    private final FollowTargetTracker tracker = new FollowTargetTracker();
    private final FollowMotionIntegrator motion = new FollowMotionIntegrator();
    private final FollowRotationSmoother rotation = new FollowRotationSmoother();
    private long lastNanos;
    private double anchorX, anchorY, anchorZ;
    private double previousAnchorX, previousAnchorY, previousAnchorZ;
    private boolean anchorInitialized;
    private boolean rebuildFromVisible;
    private boolean settleSemanticBearing;
    private double positionResponse = 10.0;
    private double rotationResponse = 14.0;
    private double lookAtMovementSpeed = 4.0;

    public void configure(double positionResponse, double rotationResponse) {
        this.positionResponse = Math.clamp(positionResponse, 0.1, 40.0);
        this.rotationResponse = Math.clamp(rotationResponse, 0.1, 40.0);
    }

    public void configureLookAtMovement(double speed) {
        lookAtMovementSpeed = Double.isFinite(speed) && speed > 0.0
                ? Math.clamp(speed, 0.05, 128.0) : 4.0;
    }

    public FollowCameraState state() { return state; }
    public FollowTargetTracker tracker() { return tracker; }
    public double anchorX() { return anchorX; }
    public double anchorY() { return anchorY; }
    public double anchorZ() { return anchorZ; }

    public void enter(Minecraft client, CameraPose visible) {
        if (state.target() == null) {
            state.setTarget(client.player);
        }
        lastNanos = 0L;
        anchorInitialized = false;
        rebuildFromVisible = true;
        settleSemanticBearing = false;
        motion.clear();
    }

    public void switchProfilePreservingPose(FollowProfile profile, CameraPose visible) {
        if (profile == state.profile()) {
            return;
        }
        state.setProfile(profile);
        motion.clear();
        settleSemanticBearing = false;
        lastNanos = 0L;
        if (anchorInitialized) {
            rebuildFramingFromPose(visible, stableTargetYaw(1.0F));
            rebuildFromVisible = false;
        } else {
            rebuildFromVisible = true;
        }
    }

    public boolean valid(Minecraft client) {
        return tracker.valid(client, state.target());
    }

    public void advance(Minecraft client, CameraPose pose, CameraInputSnapshot input) {
        Entity target = state.target();
        if (target == null) {
            return;
        }
        long now = System.nanoTime();
        double elapsed = lastNanos == 0L ? 0.0 : (now - lastNanos) * 1.0E-9;
        lastNanos = now;
        if (!Double.isFinite(elapsed) || elapsed < 0.0 || elapsed > 0.25) {
            elapsed = 0.0;
            motion.clear();
        }

        float partialTick = client.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        Vec3 targetPosition = target.getPosition(partialTick);
        anchorX = targetPosition.x;
        anchorY = targetPosition.y + target.getBbHeight() * 0.5;
        anchorZ = targetPosition.z;
        float targetYaw = stableTargetYaw(partialTick);

        if (anchorInitialized) {
            double dx = anchorX - previousAnchorX;
            double dy = anchorY - previousAnchorY;
            double dz = anchorZ - previousAnchorZ;
            if (dx * dx + dy * dy + dz * dz > TARGET_DISCONTINUITY_SQUARED) {
                if (state.profile() != FollowProfile.LOOK_AT) {
                    pose.translate(dx, dy, dz);
                }
                elapsed = 0.0;
                motion.clear();
            }
        }
        previousAnchorX = anchorX;
        previousAnchorY = anchorY;
        previousAnchorZ = anchorZ;
        anchorInitialized = true;

        if (rebuildFromVisible) {
            rebuildFramingFromPose(pose, targetYaw);
            rebuildFromVisible = false;
            return;
        }

        if (state.profile() == FollowProfile.LOOK_AT) {
            motion.moveCameraRelative(
                    pose, input, elapsed, lookAtMovementSpeed, positionResponse
            );
            lookAt(pose, motion.response(elapsed, rotationResponse), false);
            return;
        }

        if (elapsed > 0.0) {
            state.adjustDistance(FollowProfileMath.distanceDelta(
                    input.forward(), elapsed, CONTROL_DISTANCE_RATE
            ));
            state.adjustOffsets(0.0, input.vertical() * CONTROL_HEIGHT_RATE * elapsed);
            // Camera screen-right is the negative bearing direction while looking at the anchor.
            state.adjustBearing(FollowProfileMath.bearingDelta(
                    input.strafe(), elapsed, CONTROL_ORBIT_RATE
            ));
            if (input.strafe() != 0.0) {
                settleSemanticBearing = false;
            } else if (settleSemanticBearing) {
                float difference = Mth.wrapDegrees(-state.framingYaw());
                double settle = motion.response(elapsed, 3.0);
                if (Math.abs(difference) < 0.05F) {
                    state.setBearing(0.0F);
                    settleSemanticBearing = false;
                } else {
                    state.adjustBearing(difference * settle);
                }
            }
        }

        float baseYaw = FollowProfileMath.bearing(
                state.profile(), targetYaw, state.framingYaw()
        );
        double desiredX = FollowProfileMath.desiredX(
                anchorX, state.distance(), state.lateral(), baseYaw
        );
        double desiredY = anchorY + state.height();
        double desiredZ = FollowProfileMath.desiredZ(
                anchorZ, state.distance(), state.lateral(), baseYaw
        );
        double response = motion.response(elapsed, positionResponse);
        if (response > 0.0) {
            pose.setPosition(
                    pose.x() + (desiredX - pose.x()) * response,
                    pose.y() + (desiredY - pose.y()) * response,
                    pose.z() + (desiredZ - pose.z()) * response
            );
        }
        lookAt(pose, motion.response(elapsed, rotationResponse), true);
    }

    public void rotate(double yawInput, double pitchInput) {
        if (state.profile() != FollowProfile.LOOK_AT) {
            settleSemanticBearing = false;
            state.rotate((float) yawInput * 0.15F, (float) pitchInput * 0.15F);
        }
    }

    public boolean adjustDistance(double scroll) {
        if (scroll == 0.0 || state.profile() == FollowProfile.LOOK_AT) {
            return false;
        }
        state.adjustDistance(-Math.signum(scroll) * 0.5);
        return true;
    }

    public void selectTarget(Minecraft client, CameraPose rendered) {
        Entity selected = tracker.select(
                client, rendered.x(), rendered.y(), rendered.z(), rendered.yaw(), rendered.pitch()
        );
        if (selected != null) {
            state.setTarget(selected);
            anchorInitialized = false;
            rebuildFromVisible = true;
            motion.clear();
        }
    }

    public void suspend() {
        lastNanos = 0L;
        motion.clear();
    }

    public void clear() {
        state.clear();
        lastNanos = 0L;
        anchorInitialized = false;
        rebuildFromVisible = false;
        settleSemanticBearing = false;
        motion.clear();
    }

    private float stableTargetYaw(float partialTick) {
        Entity target = state.target();
        Vec3 velocity = target.getDeltaMovement();
        double horizontalSpeedSquared = velocity.x * velocity.x + velocity.z * velocity.z;
        if (horizontalSpeedSquared > 0.0025) {
            return (float) Math.toDegrees(Math.atan2(-velocity.x, velocity.z));
        }
        return target instanceof LivingEntity living
                ? living.getPreciseBodyRotation(partialTick)
                : Mth.rotLerp(partialTick, target.yRotO, target.getYRot());
    }

    private void rebuildFramingFromPose(CameraPose pose, float targetYaw) {
        if (state.profile() == FollowProfile.LOOK_AT) {
            return;
        }
        double dx = pose.x() - anchorX;
        double dz = pose.z() - anchorZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        float worldBearing = (float) Math.toDegrees(Math.atan2(dx, -dz));
        float framing = FollowProfileMath.framingFromWorldBearing(
                state.profile(), worldBearing, targetYaw
        );
        state.rebuildFraming(distance, pose.y() - anchorY, framing);
        settleSemanticBearing = state.profile() == FollowProfile.CHASE
                || state.profile() == FollowProfile.SIDE;
    }

    private void lookAt(CameraPose pose, double response, boolean allowFramingPitch) {
        double dx = anchorX - pose.x();
        double dy = anchorY - pose.y();
        double dz = anchorZ - pose.z();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontal));
        if (allowFramingPitch) {
            pitch = Mth.clamp(pitch + state.framingPitch() - 12.0F, -90.0F, 90.0F);
        }
        if (response > 0.0) {
            pose.setRotation(
                    rotation.approach(pose.yaw(), yaw, response),
                    Mth.clamp(rotation.approach(pose.pitch(), pitch, response), -90.0F, 90.0F)
            );
        }
    }
}
