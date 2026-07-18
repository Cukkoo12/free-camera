package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputRouter;
import com.cukkoo.freecamera.motion.CameraMotionIntegrator;
import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.orbit.OrbitCameraRig;
import com.cukkoo.freecamera.state.CameraSession;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.tripod.TripodCameraRig;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import com.cukkoo.freecamera.cinematic.CinematicMotionState;
import com.cukkoo.freecamera.collision.CameraCollisionResolver;
import com.cukkoo.freecamera.follow.FollowCameraRig;
import com.cukkoo.freecamera.recording.CameraRecordingController;
import com.cukkoo.freecamera.path.CameraPathController;
import net.minecraft.client.Minecraft;
import com.cukkoo.freecamera.config.FreeCameraConfigManager;

public final class CameraRenderBridge {
    private final CameraStateMachine stateMachine;
    private final CameraInputRouter inputRouter;
    private final CameraMotionIntegrator motionIntegrator;
    private final OrbitCameraRig orbitRig;
    private final TripodCameraRig tripodRig;
    private final CinematicMotionState cinematicMotionState;
    private final CameraCollisionResolver collisionResolver;
    private final FollowCameraRig followRig;
    private final CameraRecordingController recordingController;
    private final CameraPathController pathController;
    private final FreeCameraConfigManager config;
    private final CameraPose playbackDesired = new CameraPose(0,0,0,0,0);

    public CameraRenderBridge(
            CameraStateMachine stateMachine,
            CameraInputRouter inputRouter,
            CameraMotionIntegrator motionIntegrator,
            OrbitCameraRig orbitRig,
            TripodCameraRig tripodRig,
            CinematicMotionState cinematicMotionState,
            CameraCollisionResolver collisionResolver,
            FollowCameraRig followRig,
            CameraRecordingController recordingController,
            CameraPathController pathController,
            FreeCameraConfigManager config
    ) {
        this.stateMachine = stateMachine;
        this.inputRouter = inputRouter;
        this.motionIntegrator = motionIntegrator;
        this.orbitRig = orbitRig;
        this.tripodRig = tripodRig;
        this.cinematicMotionState = cinematicMotionState;
        this.collisionResolver = collisionResolver;
        this.followRig = followRig;
        this.recordingController = recordingController;
        this.pathController = pathController;
        this.config = config;
    }

    public CameraPose activePoseOrNull(Minecraft client) {
        CameraSession session = stateMachine.activeSessionOrNull();
        if (session == null) {
            return null;
        }
        CameraPose pose = session.pose();
        if(stateMachine.isCameraInputEnabled()&&pathController.isPlaying()&&pathController.isPaused()&&!pathController.isScreenSuspended())return session.renderedPose();
        if(stateMachine.isCameraInputEnabled()&&recordingController.playback().isPlaying()&&recordingController.playback().isPaused()&&!recordingController.isScreenSuspended())return session.renderedPose();
        if (pathController.applyFrame(playbackDesired, stateMachine.isCameraInputEnabled())) {
            pose.copyFrom(playbackDesired); return playbackResult(client, session, playbackDesired);
        }
        if (recordingController.applyPlayback(playbackDesired, stateMachine.isCameraInputEnabled())) {
            pose.copyFrom(playbackDesired);
            return playbackResult(client, session, playbackDesired);
        }
        CinematicMotionProfile profile = cinematicMotionState.activeProfile(session.mode());
        if (session.mode() == CameraMode.TRIPOD) {
            inputRouter.clear();
            motionIntegrator.clearMotionState();
            orbitRig.suspendMotion();
            tripodRig.applyPose(pose);
        } else if (stateMachine.isCameraInputEnabled()) {
            if (session.mode() == CameraMode.ORBIT && client.player != null) {
                motionIntegrator.clearMotionState();
                orbitRig.advance(client, pose, inputRouter.sample(client), client.player, profile);
            } else if (session.mode() == CameraMode.FOLLOW) {
                motionIntegrator.clearMotionState();
                if (!followRig.valid(client)) {
                    stateMachine.handleLostFollowTarget(client);
                    return stateMachine.activeSessionOrNull().renderedPose();
                }
                followRig.advance(client, pose, inputRouter.sample(client));
            } else {
                orbitRig.suspendMotion();
                motionIntegrator.advance(client, pose, inputRouter.sample(client), profile);
            }
        } else {
            inputRouter.clear();
            motionIntegrator.clearMotionState();
            orbitRig.suspendMotion();
        }
        applyCinematicRotation(client, session, pose);
        double anchorX = client.player == null ? pose.x() : client.player.getX();
        double anchorY = client.player == null ? pose.y() : client.player.getEyeY();
        double anchorZ = client.player == null ? pose.z() : client.player.getZ();
        if (session.mode() == CameraMode.ORBIT) anchorY += orbitRig.state().anchorOffsetY();
        if (session.mode() == CameraMode.FOLLOW) {
            anchorX = followRig.anchorX(); anchorY = followRig.anchorY(); anchorZ = followRig.anchorZ();
        }
        collisionResolver.resolve(
                client,
                stateMachine,
                session.mode(),
                pose,
                session.renderedPose(), anchorX, anchorY, anchorZ
        );
        return session.renderedPose();
    }

    private CameraPose playbackResult(Minecraft client, CameraSession session, CameraPose desired) {
        if (!config.config().playbackCollision) { session.renderedPose().copyFrom(desired); return session.renderedPose(); }
        com.cukkoo.freecamera.collision.CameraCollisionMode mode;
        try { mode=com.cukkoo.freecamera.collision.CameraCollisionMode.valueOf(config.config().playbackCollisionMode); }
        catch (IllegalArgumentException ex) { mode=com.cukkoo.freecamera.collision.CameraCollisionMode.OFF; }
        collisionResolver.resolvePlayback(client,stateMachine,mode,desired,session.renderedPose());
        return session.renderedPose();
    }

    private void applyCinematicRotation(
            Minecraft client,
            CameraSession session,
            CameraPose pose
    ) {
        if (!stateMachine.isCameraInputEnabled()
                || !cinematicMotionState.isEnabled()
                || session.mode() == CameraMode.TRIPOD) {
            return;
        }
        float yaw = session.mode() == CameraMode.ORBIT ? orbitRig.state().yaw() : pose.yaw();
        float pitch = session.mode() == CameraMode.ORBIT ? orbitRig.state().pitch() : pose.pitch();
        if (!client.isWindowActive()) {
            cinematicMotionState.suspend(yaw, pitch);
            return;
        }
        double elapsed = cinematicMotionState.rotationSmoother().elapsedSeconds(System.nanoTime());
        if (!cinematicMotionState.rotationSmoother().advance(
                yaw,
                pitch,
                CinematicMotionProfile.CINEMATIC,
                elapsed
        )) {
            return;
        }
        if (session.mode() == CameraMode.ORBIT && client.player != null) {
            orbitRig.applySmoothedRotation(
                    pose,
                    client.player,
                    cinematicMotionState.rotationSmoother().outputYaw(),
                    cinematicMotionState.rotationSmoother().outputPitch()
            );
        } else {
            pose.setRotation(
                    cinematicMotionState.rotationSmoother().outputYaw(),
                    cinematicMotionState.rotationSmoother().outputPitch()
            );
        }
    }
}
