package com.cukkoo.freecamera.input;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.roll.CameraRollController;
import com.cukkoo.freecamera.radial.CameraPrimaryKeyController;
import com.cukkoo.freecamera.zoom.CameraZoomController;
import com.cukkoo.freecamera.recording.CameraRecordingController;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class CameraKeyMappings {
    private final CameraStateMachine stateMachine;
    private final CameraRollController rollController;
    private final CameraZoomController zoomController;
    private final CameraPrimaryKeyController primaryKeyController;
    private final CameraRecordingController recordingController;
    private KeyMapping cameraControl;
    private KeyMapping directFreeCamera;
    private KeyMapping directOrbitCamera;
    private KeyMapping directTripodCamera;
    private KeyMapping resetCamera;
    private KeyMapping toggleCinematicMotion;
    private KeyMapping selectFollowTarget;
    private KeyMapping openSettings;
    private KeyMapping toggleRecording, togglePlayback;

    public CameraKeyMappings(
            CameraStateMachine stateMachine,
            CameraRollController rollController,
            CameraZoomController zoomController,
            CameraPrimaryKeyController primaryKeyController,
            CameraRecordingController recordingController
    ) {
        this.stateMachine = stateMachine;
        this.rollController = rollController;
        this.zoomController = zoomController;
        this.primaryKeyController = primaryKeyController;
        this.recordingController = recordingController;
    }

    public void register() {
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(FreeCameraClient.MOD_ID, "controls")
        );
        cameraControl = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.camera_control",
                GLFW.GLFW_KEY_F6,
                category
        ));
        directFreeCamera = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.direct_free",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        directOrbitCamera = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.direct_orbit",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        directTripodCamera = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.direct_tripod",
                GLFW.GLFW_KEY_UNKNOWN,
                category
        ));
        resetCamera = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.reset",
                GLFW.GLFW_KEY_R,
                category
        ));
        toggleCinematicMotion = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.toggle_cinematic",
                GLFW.GLFW_KEY_F9,
                category
        ));
        selectFollowTarget = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.select_follow_target", GLFW.GLFW_KEY_G, category
        ));
        openSettings = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.open_settings", GLFW.GLFW_KEY_F10, category
        ));
        toggleRecording=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.free-camera.toggle_recording",GLFW.GLFW_KEY_UNKNOWN,category));
        togglePlayback=KeyMappingHelper.registerKeyMapping(new KeyMapping("key.free-camera.toggle_playback",GLFW.GLFW_KEY_UNKNOWN,category));
        rollController.register(category, GLFW.GLFW_KEY_C, GLFW.GLFW_KEY_Z);
        zoomController.register(category);
    }

    public void processTick(Minecraft client) {
        if (openSettings.consumeClick() && client.screen == null) {
            FreeCameraClient.openSettingsScreen();
            return;
        }
        primaryKeyController.tick(client, cameraControl);
        if (primaryKeyController.isInputSuppressed()) {
            drainDirectModeKeys();
            drainWithoutTransition(resetCamera);
            drainWithoutTransition(toggleCinematicMotion);
            rollController.suspendInput();
            zoomController.suspendInput();
            return;
        }
        // Deterministic direct-shortcut priority: Free, Orbit, Tripod.
        CameraMode requestedMode = CameraModeKeyPriority.resolve(
                directFreeCamera.consumeClick(),
                directOrbitCamera.consumeClick(),
                directTripodCamera.consumeClick()
        );
        boolean cinematicTogglePressed = toggleCinematicMotion.consumeClick();

        if (requestedMode != null) {
            processModeRequest(client, requestedMode);
            drainDirectModeKeys();
            drainWithoutTransition(toggleCinematicMotion);
        } else if (cinematicTogglePressed) {
            stateMachine.toggleCinematicMotion(client);
            drainWithoutTransition(toggleCinematicMotion);
        }

        if (resetCamera.consumeClick() && stateMachine.isActive()) {
            if (!rollController.resetToZeroIfNeeded()) {
                stateMachine.resetToPlayer(client);
            }
        }
        if (selectFollowTarget.consumeClick()) stateMachine.selectFollowTarget(client);
        if (toggleRecording.consumeClick() && stateMachine.isActive()) recordingController.toggleRecording();
        if (togglePlayback.consumeClick() && stateMachine.isActive()) recordingController.togglePlayback();
    }

    public void clearQueuedTransitions() {
        primaryKeyController.drainAndBlockUntilRelease(cameraControl);
        drainDirectModeKeys();
        drainWithoutTransition(resetCamera);
        drainWithoutTransition(toggleCinematicMotion);
        drainWithoutTransition(selectFollowTarget);
        drainWithoutTransition(openSettings);
        drainWithoutTransition(toggleRecording); drainWithoutTransition(togglePlayback);
        rollController.suspendInput();
        zoomController.suspendInput();
    }

    private void processModeRequest(Minecraft client, CameraMode requestedMode) {
        CameraStateMachine.ActivationResult result = stateMachine.requestMode(client, requestedMode);
        if (result == CameraStateMachine.ActivationResult.BLOCKED_SPECTATOR) {
            CameraStateMachine.showMessage(client, "message.free-camera.activation_blocked_spectator");
        } else if (result == CameraStateMachine.ActivationResult.BLOCKED_INVALID_WORLD) {
            CameraStateMachine.showMessage(client, "message.free-camera.activation_blocked_world");
        }
        if (stateMachine.activeModeOrNull() == requestedMode) {
            primaryKeyController.rememberSuccessfulMode(requestedMode);
        }
    }

    public boolean isRadialInputSuppressed() {
        return primaryKeyController.isInputSuppressed();
    }

    public void forceClearRadial() {
        primaryKeyController.clear();
    }

    private void drainDirectModeKeys() {
        drainWithoutTransition(directFreeCamera);
        drainWithoutTransition(directOrbitCamera);
        drainWithoutTransition(directTripodCamera);
    }

    private static void drainWithoutTransition(KeyMapping mapping) {
        if (mapping == null) {
            return;
        }
        while (mapping.consumeClick()) {
            // Deliberately discard queued clicks; no state transition occurs in this loop.
        }
    }
}
