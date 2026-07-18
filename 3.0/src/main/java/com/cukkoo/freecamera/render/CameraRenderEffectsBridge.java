package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import com.cukkoo.freecamera.cinematic.CinematicMotionState;
import com.cukkoo.freecamera.mixin.CameraProjectionAccessor;
import com.cukkoo.freecamera.roll.CameraOrientationBridge;
import com.cukkoo.freecamera.roll.CameraRollController;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.zoom.CameraProjectionBridge;
import com.cukkoo.freecamera.zoom.CameraZoomController;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import com.cukkoo.freecamera.recording.CameraRecordingController;
import com.cukkoo.freecamera.config.FreeCameraConfigManager;
import com.cukkoo.freecamera.path.CameraPathController;

public final class CameraRenderEffectsBridge {
    private final CameraStateMachine stateMachine;
    private final CinematicMotionState cinematicMotionState;
    private final CameraRollController rollController;
    private final CameraZoomController zoomController;
    private final CameraOrientationBridge orientationBridge;
    private final CameraProjectionBridge projectionBridge;
    private final CanonicalWorldView canonicalWorldView = new CanonicalWorldView();
    private final CameraRecordingController recordingController;
    private final FreeCameraConfigManager config;
    private final CameraPathController pathController;

    public CameraRenderEffectsBridge(
            CameraStateMachine stateMachine,
            CinematicMotionState cinematicMotionState,
            CameraRollController rollController,
            CameraZoomController zoomController,
            CameraOrientationBridge orientationBridge,
            CameraProjectionBridge projectionBridge,
            CameraRecordingController recordingController,
            FreeCameraConfigManager config,
            CameraPathController pathController
    ) {
        this.stateMachine = stateMachine;
        this.cinematicMotionState = cinematicMotionState;
        this.rollController = rollController;
        this.zoomController = zoomController;
        this.orientationBridge = orientationBridge;
        this.projectionBridge = projectionBridge;
        this.recordingController = recordingController; this.config = config; this.pathController = pathController;
    }

    public void prepareFrame(Minecraft client, Camera camera) {
        CameraMode mode = stateMachine.activeModeOrNull();
        if (mode == null) {
            canonicalWorldView.clear();
            return;
        }
        CinematicMotionProfile cameraProfile = cinematicMotionState.activeProfile(mode);
        CinematicMotionProfile zoomProfile = cinematicMotionState.isEnabled()
                ? CinematicMotionProfile.CINEMATIC
                : CinematicMotionProfile.RESPONSIVE;
        rollController.advance(client, mode, cameraProfile);
        zoomController.advance(client, zoomProfile);
        if (pathController.hasOutput()) {
            rollController.state().setPlaybackRoll(pathController.outputRoll());
            zoomController.state().setPlaybackMultiplier(pathController.outputZoom());
        } else if (recordingController.playback().hasOutput()) {
            rollController.state().setPlaybackRoll(recordingController.playback().outputRoll());
            zoomController.state().setPlaybackMultiplier(recordingController.playback().outputZoom());
        }
        var session = stateMachine.activeSessionOrNull();
        if (session != null && recordingController.sample(session.renderedPose(), rollController.state().currentRoll(),
                zoomController.state().currentMultiplier(), mode, config.config().recordingRate, config.config().recordingMaxSeconds,config.config().maximumRecordingSamples))
            client.gui.setOverlayMessage(net.minecraft.network.chat.Component.translatable("message.free-camera.recording_limit"),false);
        CameraProjectionAccessor cameraAccessor = (CameraProjectionAccessor) camera;
        camera.getViewRotationMatrix(canonicalWorldView.mutableView());
        cameraAccessor.freecamera$projection().getMatrix(
                canonicalWorldView.mutableProjection()
        );
        canonicalWorldView.prepare(
                canonicalWorldView.view(),
                canonicalWorldView.projection(),
                camera.yRot(),
                camera.xRot(),
                rollController.state().currentRoll(),
                zoomController.state().currentMultiplier(),
                orientationBridge,
                projectionBridge
        );
        orientationBridge.updateFrustum(
                cameraAccessor.freecamera$cullFrustum(),
                canonicalWorldView.projection(),
                canonicalWorldView.view()
        );
    }

    public void applyProjection(CameraRenderState cameraState) {
        if (!canonicalWorldView.isReady() || stateMachine.activeModeOrNull() == null) {
            return;
        }
        projectionBridge.apply(cameraState, zoomController.state().currentMultiplier());
    }

    public Matrix4fc selectCanonicalWorldView(Matrix4fc vanillaView) {
        if (stateMachine.activeModeOrNull() == null) {
            return vanillaView;
        }
        return canonicalWorldView.selectViewOrVanilla(vanillaView);
    }

    public void clear() {
        canonicalWorldView.clear();
    }
}
