package com.cukkoo.freecamera;

import com.cukkoo.freecamera.input.CameraKeyMappings;
import com.cukkoo.freecamera.cinematic.CinematicMotionState;
import com.cukkoo.freecamera.collision.CameraCollisionResolver;
import com.cukkoo.freecamera.follow.FollowCameraRig;
import com.cukkoo.freecamera.config.FreeCameraConfigManager;
import com.cukkoo.freecamera.gui.CameraSettingsScreen;
import com.cukkoo.freecamera.hud.CameraStatusHud;
import com.cukkoo.freecamera.recording.CameraRecordingController;
import com.cukkoo.freecamera.path.CameraPathController;
import com.cukkoo.freecamera.path.CameraStudioState;
import com.cukkoo.freecamera.path.CameraKeyframe;
import com.cukkoo.freecamera.path.PathEasing;
import com.cukkoo.freecamera.recording.CameraRecordingStorage;
import com.cukkoo.freecamera.path.CameraPathStorage;
import net.minecraft.client.gui.screens.Screen;
import com.cukkoo.freecamera.input.CameraInputRouter;
import com.cukkoo.freecamera.input.PlayerInputSuppressor;
import com.cukkoo.freecamera.lifecycle.CameraLifecycleManager;
import com.cukkoo.freecamera.motion.CameraMotionIntegrator;
import com.cukkoo.freecamera.orbit.OrbitCameraRig;
import com.cukkoo.freecamera.render.CameraRenderBridge;
import com.cukkoo.freecamera.render.CameraRenderEffectsBridge;
import com.cukkoo.freecamera.roll.CameraOrientationBridge;
import com.cukkoo.freecamera.roll.CameraRollController;
import com.cukkoo.freecamera.zoom.CameraProjectionBridge;
import com.cukkoo.freecamera.zoom.CameraZoomController;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.tripod.TripodCameraRig;
import com.cukkoo.freecamera.radial.CameraPrimaryKeyController;
import com.cukkoo.freecamera.radial.CameraRadialMenuController;
import com.cukkoo.freecamera.radial.CameraRadialMenuRenderer;
import com.cukkoo.freecamera.radial.CameraRadialMenuState;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FreeCameraClient implements ClientModInitializer {
    public static final String MOD_ID = "free-camera";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static FreeCameraClient instance;

    private final CameraStateMachine stateMachine;
    private final CameraInputRouter inputRouter;
    private final PlayerInputSuppressor playerInputSuppressor;
    private final CameraMotionIntegrator motionIntegrator;
    private final OrbitCameraRig orbitRig;
    private final TripodCameraRig tripodRig;
    private final CinematicMotionState cinematicMotionState;
    private final CameraRollController rollController;
    private final CameraZoomController zoomController;
    private final CameraCollisionResolver collisionResolver;
    private final FollowCameraRig followRig;
    private final FreeCameraConfigManager configManager;
    private final CameraStatusHud statusHud;
    private final CameraRecordingController recordingController;
    private final CameraPathController pathController;
    private final CameraStudioState studioState;
    private final CameraRecordingStorage recordingStorage;
    private final CameraPathStorage pathStorage;
    private final CameraRenderBridge renderBridge;
    private final CameraRenderEffectsBridge renderEffectsBridge;
    private final CameraKeyMappings keyMappings;
    private final CameraLifecycleManager lifecycleManager;
    private final CameraPrimaryKeyController primaryKeyController;
    private final CameraRadialMenuController radialMenuController;
    private final CameraRadialMenuRenderer radialMenuRenderer;

    public FreeCameraClient() {
        orbitRig = new OrbitCameraRig();
        tripodRig = new TripodCameraRig();
        cinematicMotionState = new CinematicMotionState();
        rollController = new CameraRollController();
        zoomController = new CameraZoomController();
        collisionResolver = new CameraCollisionResolver();
        followRig = new FollowCameraRig();
        configManager = new FreeCameraConfigManager();
        recordingController = new CameraRecordingController();
        pathController = new CameraPathController();
        studioState = new CameraStudioState();
        recordingStorage = new CameraRecordingStorage(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        pathStorage = new CameraPathStorage(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        stateMachine = new CameraStateMachine(orbitRig, tripodRig, cinematicMotionState);
        stateMachine.setFollowRig(followRig);
        CameraRadialMenuState radialMenuState = new CameraRadialMenuState();
        radialMenuController = new CameraRadialMenuController(radialMenuState);
        primaryKeyController = new CameraPrimaryKeyController(stateMachine, radialMenuController);
        radialMenuRenderer = new CameraRadialMenuRenderer(radialMenuState, stateMachine);
        inputRouter = new CameraInputRouter(stateMachine, primaryKeyController::isInputSuppressed);
        playerInputSuppressor = new PlayerInputSuppressor(
                stateMachine,
                primaryKeyController::isInputSuppressed
        );
        motionIntegrator = new CameraMotionIntegrator();
        renderBridge = new CameraRenderBridge(
                stateMachine,
                inputRouter,
                motionIntegrator,
                orbitRig,
                tripodRig,
                cinematicMotionState,
                collisionResolver,
                followRig,
                recordingController,
                pathController,
                configManager
        );
        renderEffectsBridge = new CameraRenderEffectsBridge(
                stateMachine,
                cinematicMotionState,
                rollController,
                zoomController,
                new CameraOrientationBridge(),
                new CameraProjectionBridge(),
                recordingController,
                configManager,
                pathController
        );
        keyMappings = new CameraKeyMappings(
                stateMachine,
                rollController,
                zoomController,
                primaryKeyController,
                recordingController
        );
        lifecycleManager = new CameraLifecycleManager(stateMachine, keyMappings);
        statusHud = new CameraStatusHud(stateMachine,configManager,motionIntegrator,orbitRig,followRig,collisionResolver,cinematicMotionState,rollController,zoomController,recordingController,pathController);
        stateMachine.setTransientStateReset(this::clearTransientSystems);
        stateMachine.setVisualEffectLifecycle(
                this::clearVisualEffects,
                this::suspendVisualEffects
        );
    }

    @Override
    public void onInitializeClient() {
        try {
            if (instance != null) {
                throw new IllegalStateException("Free Camera client initialized twice");
            }

            instance = this;
            configManager.load();
            applyConfiguration();
            keyMappings.register();
            radialMenuRenderer.register();
            statusHud.register();
            lifecycleManager.register();
            LOGGER.info("Free Camera 3.0 client initialized");
        } catch (RuntimeException exception) {
            instance = null;
            LOGGER.error("Free Camera startup initialization failed", exception);
            throw exception;
        }
    }

    public static CameraStateMachine stateMachineOrNull() {
        return instance == null ? null : instance.stateMachine;
    }

    public static CameraRenderBridge renderBridgeOrNull() {
        return instance == null ? null : instance.renderBridge;
    }

    public static CameraRenderEffectsBridge renderEffectsBridgeOrNull() {
        return instance == null ? null : instance.renderEffectsBridge;
    }

    public static PlayerInputSuppressor playerInputSuppressorOrNull() {
        return instance == null ? null : instance.playerInputSuppressor;
    }

    public static boolean isRadialInputSuppressed() {
        return instance != null && instance.primaryKeyController.isInputSuppressed();
    }

    public static boolean isRadialMenuOpen() {
        return instance != null && instance.radialMenuController.state().isOpen();
    }

    public static void accumulateRadialMouse(double deltaX, double deltaY) {
        if (instance == null || !instance.radialMenuController.state().isOpen()) {
            return;
        }
        instance.radialMenuController.accumulateMouse(
                deltaX,
                deltaY,
                Minecraft.getInstance().getWindow().getGuiScale()
        );
    }

    public static void cancelRadialMenu() {
        if (instance != null && instance.radialMenuController.state().isOpen()) {
            instance.primaryKeyController.cancelUntilPrimaryRelease();
        }
    }

    public static void markCollisionWorldDirty() {
        if (instance != null) {
            instance.collisionResolver.markWorldCollisionDirty();
        }
    }

    public static Screen createSettingsScreen(Screen parent) {
        return new CameraSettingsScreen(parent, instance.configManager);
    }

    public static void openSettingsScreen() {
        Minecraft client = Minecraft.getInstance();
        if (instance != null && client.screen == null) client.setScreen(createSettingsScreen(null));
    }

    public static void applyConfiguration() {
        if(instance==null)return;var c=instance.configManager.config();
        instance.primaryKeyController.setHoldDurationMillis(c.radialHoldMillis);
        instance.primaryKeyController.configurePreferredMode(c.lastMode,c.rememberLastMode,mode->{if(instance.configManager.config().rememberLastMode){instance.configManager.config().lastMode=mode;try{instance.configManager.save();}catch(java.io.IOException error){LOGGER.warn("Could not persist last-used camera mode",error);}}});
        instance.radialMenuController.state().setDeadZoneRadius(c.radialDeadZone);
        instance.collisionResolver.state().setModeFor(com.cukkoo.freecamera.api.CameraMode.FREE_CAMERA,c.freeCollision);
        instance.collisionResolver.state().setModeFor(com.cukkoo.freecamera.api.CameraMode.ORBIT,c.orbitCollision);
        instance.collisionResolver.state().setModeFor(com.cukkoo.freecamera.api.CameraMode.FOLLOW,c.followCollision);
        com.cukkoo.freecamera.follow.FollowProfile previousFollowProfile=instance.followRig.state().profile();
        instance.followRig.state().configure(c.followDistance,c.followHeight,c.followLateral);
        var active=instance.stateMachine.activeSessionOrNull();
        if(active!=null&&active.mode()==com.cukkoo.freecamera.api.CameraMode.FOLLOW&&previousFollowProfile!=c.followProfile){
            instance.followRig.switchProfilePreservingPose(c.followProfile,active.renderedPose());
        }else instance.followRig.state().setProfile(c.followProfile);
        instance.followRig.tracker().configure(c.followRange,c.followPlayers,c.followMobs,c.followBoats,c.followMinecarts,c.followArmorStands,c.followVehicles);
        instance.followRig.configure(c.followPositionSmoothing,c.followRotationSmoothing);
        double followMovementSpeed=c.customSpeed;
        if(!"CUSTOM".equals(c.speedPreset))try{followMovementSpeed=com.cukkoo.freecamera.motion.SpeedPreset.valueOf(c.speedPreset).blocksPerSecond();}catch(IllegalArgumentException ignored){followMovementSpeed=4.0;}
        instance.followRig.configureLookAtMovement(followMovementSpeed);
        instance.orbitRig.configure(c.orbitRadius,c.orbitSensitivity);
        instance.motionIntegrator.configure(c.speedPreset,c.customSpeed,c.acceleration,c.deceleration,c.cinematicAcceleration,c.cinematicDeceleration,c.maximumFrameTime,c.movementSubsteps);
        instance.collisionResolver.configure(c.collisionMargin);
        instance.rollController.configure(c.rollSpeed,c.rollSmoothing,c.cinematicRollSmoothing);
        instance.zoomController.configure(c.zoomMultiplier,c.zoomSmoothing,c.cinematicZoomSmoothing);
        instance.recordingController.playback().setSpeed(c.defaultPlaybackSpeed);instance.pathController.setSpeed(c.defaultPlaybackSpeed);
        instance.cinematicMotionState.setEnabledPreference(c.cinematicEnabled);
    }
    public static void persistCinematicPreference(boolean enabled){if(instance==null)return;instance.configManager.config().cinematicEnabled=enabled;try{instance.configManager.save();}catch(java.io.IOException error){LOGGER.warn("Could not persist Cinematic preference",error);}}

    public static void followLocalPlayer(){if(instance!=null&&Minecraft.getInstance().player!=null)instance.followRig.state().setTarget(Minecraft.getInstance().player);}
    public static void clearFollowTarget(){if(instance!=null){instance.followRig.state().setTarget(null);instance.stateMachine.handleLostFollowTarget(Minecraft.getInstance());}}
    private static final net.minecraft.network.chat.Component NO_FOLLOW_TARGET=net.minecraft.network.chat.Component.literal("None");
    public static String followTargetName(){return followTargetComponent().getString();}
    public static net.minecraft.network.chat.Component followTargetComponent(){if(instance==null||instance.followRig.state().target()==null)return NO_FOLLOW_TARGET;return instance.followRig.state().target().getDisplayName();}
    public static net.minecraft.world.entity.Entity followTargetEntity(){return instance==null?null:instance.followRig.state().target();}
    public static void startRecording(){if(instance!=null&&instance.stateMachine.isActive())instance.recordingController.startRecording();}
    public static void stopRecording(){if(instance!=null)instance.recordingController.stopRecording();}
    public static void playLastRecording(){if(instance!=null&&instance.stateMachine.isActive())instance.recordingController.playLast();}
    public static void stopPlayback(){if(instance!=null)instance.recordingController.playback().stop();}
    public static void newPath(){if(instance!=null){instance.studioState.newPath();try{instance.studioState.path().setInterpolation(com.cukkoo.freecamera.path.PathInterpolation.valueOf(instance.configManager.config().pathInterpolation));}catch(IllegalArgumentException ignored){}}}
    public static void addCurrentKeyframe(){if(instance==null||instance.studioState.path().size()>=instance.configManager.config().maximumPathKeyframes)return;var session=instance.stateMachine.activeSessionOrNull();if(session==null)return;var p=session.renderedPose();var path=instance.studioState.path();double roll=instance.rollController.state().currentRoll();if(path.size()>0){double previous=path.get(path.size()-1).roll();roll=previous+com.cukkoo.freecamera.roll.CameraRollState.normalizeDegrees(roll-com.cukkoo.freecamera.roll.CameraRollState.normalizeDegrees(previous));}PathEasing easing;try{easing=PathEasing.valueOf(instance.configManager.config().pathEasing);}catch(IllegalArgumentException ignored){easing=PathEasing.CINEMATIC;}if(path.add(new CameraKeyframe(p.x(),p.y(),p.z(),p.yaw(),p.pitch(),roll,instance.zoomController.state().currentMultiplier(),1,easing,0)))instance.studioState.markDirty();}
    public static void playPath(){if(instance==null||instance.studioState.path().size()<2)return;Minecraft client=Minecraft.getInstance();if(!instance.stateMachine.isActive()&&client.screen==null)instance.stateMachine.requestMode(client,com.cukkoo.freecamera.api.CameraMode.FREE_CAMERA);if(instance.stateMachine.isActive())instance.pathController.play(instance.studioState.path());}
    public static void stopPath(){if(instance!=null)instance.pathController.stop();}
    public static CameraRecordingController recordingController(){return instance.recordingController;}
    public static CameraPathController pathController(){return instance.pathController;}
    public static CameraStudioState studioState(){return instance.studioState;}
    public static CameraRecordingStorage recordingStorage(){return instance.recordingStorage;}
    public static CameraPathStorage pathStorage(){return instance.pathStorage;}
    public static com.cukkoo.freecamera.state.CameraSession activeSession(){return instance==null?null:instance.stateMachine.activeSessionOrNull();}
    public static double currentRoll(){return instance==null?0:instance.rollController.state().currentRoll();}public static double currentZoom(){return instance==null?1:instance.zoomController.state().currentMultiplier();}
    public static boolean openConfigFolder(){try{net.minecraft.util.Util.getPlatform().openPath(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());return true;}catch(RuntimeException ignored){return false;}}
    private static boolean activeVisual(){return instance!=null&&instance.stateMachine.isActive();}public static boolean hideHandEnabled(){return activeVisual()&&instance.configManager.config().hideHand;}public static boolean hideCrosshairEnabled(){return activeVisual()&&instance.configManager.config().hideCrosshair;}public static boolean hideHudEnabled(){return activeVisual()&&instance.configManager.config().hideHud;}public static boolean suppressViewBobbingEnabled(){return activeVisual()&&instance.configManager.config().suppressViewBobbing;}public static boolean suppressHurtCameraEnabled(){return activeVisual()&&instance.configManager.config().suppressHurtCamera;}
    public static double cameraMouseSensitivity(){return instance==null?1:instance.configManager.config().mouseSensitivity;}

    public static boolean handleSpeedPresetScroll(long window, double verticalScroll) {
        if (instance == null || verticalScroll == 0.0) {
            return false;
        }
        Minecraft client = Minecraft.getInstance();
        if (window != client.getWindow().handle()
                || !instance.stateMachine.shouldInterceptCameraMouseInput()
                || client.screen != null) {
            return false;
        }
        if (instance.stateMachine.activeModeOrNull() == com.cukkoo.freecamera.api.CameraMode.ORBIT) {
            if (instance.orbitRig.requestRadiusStep(verticalScroll)) {
                client.gui.setOverlayMessage(net.minecraft.network.chat.Component.translatable(
                        "message.free-camera.orbit_radius",
                        formatRadius(instance.orbitRig.requestedRadiusForMessage())
                ), false);
            }
        } else if (instance.stateMachine.activeModeOrNull() == com.cukkoo.freecamera.api.CameraMode.FOLLOW) {
            instance.followRig.adjustDistance(verticalScroll);
        } else {
            instance.motionIntegrator.changeSpeedPreset(client, verticalScroll);
        }
        return true;
    }

    private static String formatRadius(double radius) {
        double rounded = Math.rint(radius * 10.0) / 10.0;
        return rounded == Math.rint(rounded)
                ? Long.toString((long) rounded)
                : Double.toString(rounded);
    }

    private void clearTransientSystems() {
        inputRouter.clear();
        motionIntegrator.clearMotionState();
        playerInputSuppressor.clearTransientState();
    }

    private void clearVisualEffects() {
        collisionResolver.clear();
        recordingController.clear();
        pathController.clear();
        rollController.clear();
        zoomController.clear();
        renderEffectsBridge.clear();
    }

    private void suspendVisualEffects() {
        collisionResolver.suspend();
        recordingController.suspend();
        pathController.suspendForScreen();
        rollController.suspendInput();
        zoomController.suspendInput();
    }
}
