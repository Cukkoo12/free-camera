package com.cukkoo.freecamera.state;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.lifecycle.ClientIdentitySnapshot;
import com.cukkoo.freecamera.input.CameraInputPolicy;
import com.cukkoo.freecamera.cinematic.CinematicMotionState;
import com.cukkoo.freecamera.orbit.OrbitCameraRig;
import com.cukkoo.freecamera.tripod.TripodCameraRig;
import com.cukkoo.freecamera.follow.FollowCameraRig;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

public final class CameraStateMachine {
    public enum ActivationResult {
        ACTIVATED,
        BLOCKED_SPECTATOR,
        BLOCKED_INVALID_WORLD
    }

    public enum CleanupCause {
        USER_TOGGLE,
        DISCONNECTED,
        CLIENT_SHUTDOWN,
        LEVEL_MISSING,
        PLAYER_MISSING,
        CONNECTION_MISSING,
        LEVEL_REPLACED,
        PLAYER_REPLACED,
        PLAYER_UUID_CHANGED,
        CONNECTION_REPLACED,
        DIMENSION_CHANGED,
        PLAYER_DIED,
        SPECTATOR_ENTERED,
        PERSPECTIVE_CHANGED
    }

    private CameraState state = CameraState.INACTIVE;
    private CameraSession session;
    private boolean mouseInputConsumed;
    private boolean cameraInputEnabled;
    private Runnable transientStateReset = () -> { };
    private Runnable visualEffectClear = () -> { };
    private Runnable visualEffectSuspend = () -> { };
    private final OrbitCameraRig orbitRig;
    private final TripodCameraRig tripodRig;
    private final CinematicMotionState cinematicMotionState;
    private FollowCameraRig followRig;

    public CameraStateMachine(
            OrbitCameraRig orbitRig,
            TripodCameraRig tripodRig,
            CinematicMotionState cinematicMotionState
    ) {
        this.orbitRig = orbitRig;
        this.tripodRig = tripodRig;
        this.cinematicMotionState = cinematicMotionState;
    }

    public CameraState state() {
        return state;
    }

    public void setFollowRig(FollowCameraRig followRig) { this.followRig = followRig; }

    public boolean isActive() {
        return state == CameraState.ACTIVE && session != null;
    }

    public CameraSession activeSessionOrNull() {
        return isActive() ? session : null;
    }

    public CameraMode activeModeOrNull() {
        return isActive() ? session.mode() : null;
    }

    public boolean isCameraInputEnabled() {
        return isActive() && cameraInputEnabled;
    }

    public boolean shouldInterceptCameraMouseInput() {
        return isCameraInputEnabled()
                && CameraInputPolicy.cameraReceivesMovementAndMouse(session.mode());
    }

    public boolean shouldSuppressPlayerMovementInput() {
        return isCameraInputEnabled()
                && CameraInputPolicy.playerMovementIsSuppressed(session.mode());
    }

    public void setTransientStateReset(Runnable transientStateReset) {
        this.transientStateReset = transientStateReset;
    }

    public void setVisualEffectLifecycle(Runnable clear, Runnable suspend) {
        visualEffectClear = clear;
        visualEffectSuspend = suspend;
    }

    public void toggleCinematicMotion(Minecraft client) {
        if (!isCameraInputEnabled() || client.screen != null) {
            return;
        }
        CameraMode mode = session.mode();
        if (mode == CameraMode.TRIPOD) {
            return;
        }
        resetTransientState();
        orbitRig.suspendMotion();
        float yaw = mode == CameraMode.ORBIT ? orbitRig.state().yaw() : session.pose().yaw();
        float pitch = mode == CameraMode.ORBIT ? orbitRig.state().pitch() : session.pose().pitch();
        if (cinematicMotionState.toggle(mode, yaw, pitch)) {
            FreeCameraClient.persistCinematicPreference(cinematicMotionState.isEnabled());
            showMessage(client, cinematicMotionState.isEnabled()
                    ? "message.free-camera.cinematic_on"
                    : "message.free-camera.cinematic_off");
        }
    }

    public ActivationResult requestMode(Minecraft client, CameraMode requestedMode) {
        CameraModeTransition.Action action = CameraModeTransition.resolve(
                activeModeOrNull(),
                requestedMode
        );
        if (action == CameraModeTransition.Action.DEACTIVATE) {
            deactivate(client, CleanupCause.USER_TOGGLE, true);
            return ActivationResult.ACTIVATED;
        }
        if (action == CameraModeTransition.Action.SWITCH) {
            switchMode(client, requestedMode);
            return ActivationResult.ACTIVATED;
        }
        return activate(client, requestedMode);
    }

    private ActivationResult activate(Minecraft client, CameraMode requestedMode) {
        if (isActive()) {
            return ActivationResult.ACTIVATED;
        }

        LocalPlayer player = client.player;
        if (player != null && player.isSpectator()) {
            return ActivationResult.BLOCKED_SPECTATOR;
        }
        if (client.level == null || player == null || client.getConnection() == null
                || !player.isAlive() || client.screen != null) {
            return ActivationResult.BLOCKED_INVALID_WORLD;
        }

        Camera camera = client.gameRenderer.getMainCamera();
        if (camera == null || !camera.isInitialized()) {
            return ActivationResult.BLOCKED_INVALID_WORLD;
        }

        resetTransientState();
        orbitRig.clear();
        tripodRig.clear();
        cinematicMotionState.clear();
        visualEffectClear.run();
        CameraPose pose = new CameraPose(
                camera.position().x,
                camera.position().y,
                camera.position().z,
                camera.yRot(),
                camera.xRot()
        );
        session = new CameraSession(requestedMode, pose, ClientIdentitySnapshot.capture(client));
        if (requestedMode == CameraMode.ORBIT) {
            orbitRig.enterFromPose(pose, player);
        } else if (requestedMode == CameraMode.TRIPOD) {
            tripodRig.enterFromPose(pose);
        } else if (requestedMode == CameraMode.FOLLOW && followRig != null) {
            followRig.enter(client, pose);
        }
        state = CameraState.ACTIVE;
        cameraInputEnabled = true;
        mouseInputConsumed = false;
        showModeMessage(client, requestedMode);
        return ActivationResult.ACTIVATED;
    }

    public void resetToPlayer(Minecraft client) {
        if (!isActive() || client.player == null) {
            return;
        }
        if (session.mode() == CameraMode.TRIPOD) {
            session.pose().set(
                    client.player.getEyePosition(),
                    client.player.getYRot(),
                    client.player.getXRot()
            );
            tripodRig.enterFromPose(session.pose());
        } else if (session.mode() == CameraMode.ORBIT) {
            orbitRig.resetToDefault(session.pose(), client.player);
        } else if (session.mode() == CameraMode.FOLLOW && followRig != null) {
            session.pose().set(
                    client.player.getEyePosition(),
                    client.player.getYRot(),
                    client.player.getXRot()
            );
            followRig.state().setTarget(client.player);
            followRig.enter(client, session.pose());
        } else {
            session.pose().set(client.player.getEyePosition(), client.player.getYRot(), client.player.getXRot());
        }
        resetTransientState();
        prepareCinematicForCurrentMode();
    }

    public boolean rotateCamera(Minecraft client, double yawInput, double pitchInput) {
        if (!shouldInterceptCameraMouseInput()) {
            return false;
        }
        if (cinematicMotionState.isEnabled()) {
            float currentYaw = session.mode() == CameraMode.ORBIT
                    ? orbitRig.state().yaw()
                    : session.pose().yaw();
            float currentPitch = session.mode() == CameraMode.ORBIT
                    ? orbitRig.state().pitch()
                    : session.pose().pitch();
            cinematicMotionState.rotationSmoother().addTargetDelta(
                    currentYaw,
                    currentPitch,
                    yawInput * 0.15,
                    pitchInput * 0.15
            );
            if (session.mode() == CameraMode.ORBIT) {
                cinematicMotionState.rotationSmoother().clampPitchTarget(
                        -com.cukkoo.freecamera.orbit.OrbitCameraState.MAX_VERTICAL_ANGLE,
                        com.cukkoo.freecamera.orbit.OrbitCameraState.MAX_VERTICAL_ANGLE
                );
            }
        } else if (session.mode() == CameraMode.ORBIT) {
            if (client.player == null) {
                return false;
            }
            orbitRig.rotateFromMouse(session.pose(), client.player, yawInput, pitchInput);
        } else if (session.mode() == CameraMode.FOLLOW && followRig != null) {
            followRig.rotate(yawInput, pitchInput);
        } else {
            session.pose().rotate(yawInput, pitchInput);
        }
        mouseInputConsumed = true;
        return true;
    }

    private void switchMode(Minecraft client, CameraMode requestedMode) {
        if (!isCameraInputEnabled() || client.screen != null || client.player == null) {
            return;
        }
        session.prepareModeTransitionFromRendered();
        resetTransientState();
        visualEffectSuspend.run();
        if (requestedMode == CameraMode.ORBIT) {
            orbitRig.enterFromPose(session.pose(), client.player);
            tripodRig.leave();
        } else if (requestedMode == CameraMode.TRIPOD) {
            tripodRig.enterFromPose(session.pose());
            orbitRig.leaveToFree();
        } else if (requestedMode == CameraMode.FOLLOW && followRig != null) {
            followRig.enter(client, session.pose());
            orbitRig.leaveToFree();
            tripodRig.leave();
        } else {
            orbitRig.leaveToFree();
            tripodRig.leave();
        }
        session.setMode(requestedMode);
        prepareCinematicForCurrentMode();
        showModeMessage(client, requestedMode);
    }

    public ClientIdentitySnapshot.ValidationResult validate(Minecraft client) {
        if (!isActive()) {
            return ClientIdentitySnapshot.ValidationResult.VALID;
        }
        return session.identity().validate(client);
    }

    public void handleLostFollowTarget(Minecraft client) {
        if (!isActive() || session.mode() != CameraMode.FOLLOW) return;
        session.prepareModeTransitionFromRendered();
        if (followRig != null) followRig.clear();
        session.setMode(CameraMode.FREE_CAMERA);
        resetTransientState();
        showMessage(client, "message.free-camera.follow_target_lost");
    }

    public void selectFollowTarget(Minecraft client) {
        if (!isCameraInputEnabled() || session.mode() != CameraMode.FOLLOW || followRig == null) return;
        var before = followRig.state().target();
        followRig.selectTarget(client, session.renderedPose());
        if (followRig.state().target() == before) {
            showMessage(client, "message.free-camera.follow_target_not_found");
        }
    }

    public void deactivate(Minecraft client, CleanupCause cause, boolean notify) {
        if (!isActive()) {
            clearTransientState();
            return;
        }

        if (isUnexpectedIdentityCleanup(cause)) {
            FreeCameraClient.LOGGER.warn("Free Camera forced cleanup after invalid session identity: {}", cause);
        }

        orbitRig.clear();
        tripodRig.clear();
        cinematicMotionState.clear();
        if (followRig != null) followRig.clear();
        visualEffectClear.run();
        session.clear();
        session = null;
        state = CameraState.INACTIVE;
        cameraInputEnabled = false;
        clearTransientState();
        resetTransientState();

        if (notify) {
            showMessage(client, "message.free-camera.deactivated");
        }
    }

    public void beginClientTick() {
        mouseInputConsumed = false;
    }

    public void clearTransientState() {
        mouseInputConsumed = false;
    }

    public void resumeCameraInput() {
        cameraInputEnabled = isActive();
    }

    public void suspendCameraInput() {
        cameraInputEnabled = false;
        orbitRig.suspendMotion();
        if (followRig != null) followRig.suspend();
        visualEffectSuspend.run();
        if (isActive()) {
            float yaw = session.mode() == CameraMode.ORBIT
                    ? orbitRig.state().yaw()
                    : session.pose().yaw();
            float pitch = session.mode() == CameraMode.ORBIT
                    ? orbitRig.state().pitch()
                    : session.pose().pitch();
            cinematicMotionState.suspend(yaw, pitch);
        }
        clearTransientState();
        resetTransientState();
    }

    boolean mouseInputConsumedForDevelopmentCheck() {
        return mouseInputConsumed;
    }

    private static boolean isUnexpectedIdentityCleanup(CleanupCause cause) {
        return switch (cause) {
            case LEVEL_REPLACED, PLAYER_REPLACED, PLAYER_UUID_CHANGED,
                    CONNECTION_REPLACED, DIMENSION_CHANGED -> true;
            default -> false;
        };
    }

    private void resetTransientState() {
        transientStateReset.run();
    }

    private void prepareCinematicForCurrentMode() {
        CameraMode mode = session.mode();
        float yaw = mode == CameraMode.ORBIT ? orbitRig.state().yaw() : session.pose().yaw();
        float pitch = mode == CameraMode.ORBIT ? orbitRig.state().pitch() : session.pose().pitch();
        cinematicMotionState.prepareMode(mode, yaw, pitch);
    }

    public static void showMessage(Minecraft client, String translationKey) {
        client.gui.setOverlayMessage(Component.translatable(translationKey), false);
    }

    private static void showModeMessage(Minecraft client, CameraMode mode) {
        String key = switch (mode) {
            case FREE_CAMERA -> "message.free-camera.mode_free";
            case ORBIT -> "message.free-camera.mode_orbit";
            case TRIPOD -> "message.free-camera.mode_tripod";
            case FOLLOW -> "message.free-camera.mode_follow";
        };
        showMessage(client, key);
    }
}
