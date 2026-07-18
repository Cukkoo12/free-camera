package com.cukkoo.freecamera;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.cukkoo.freecamera.state.CameraMode;
import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Handles activation, deactivation, camera movement, zoom, cinematic,
 * and tripod for both freecam and orbit modes.
 *
 * <p>The player entity is never moved during freecam/orbit. NoClip is
 * achieved by setting {@code player.noPhysics = true}.</p>
 *
 * <p>WASD movement for freecam and spherical-coordinate orbit updates
 * are calculated every render frame inside
 * {@link com.cukkoo.freecamera.mixin.CameraMixin} using the render-
 * loop tick delta, eliminating stutter from 20 TPS updates.</p>
 */
public final class FreecamController {

    private static final float BASE_SPEED    = 0.05f;
    private static final float MIN_SPEED     = 0.01f;
    private static final float MAX_SPEED     = 20.0f;

    private static float currentSpeed = BASE_SPEED;

    private FreecamController() { }

    /** Exposed for CameraMixin (render-loop movement). */
    public static float getCurrentSpeed() {
        return currentSpeed;
    }

    // ── Activation / deactivation ──────────────────────────────────

    public static void toggle(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (state.isActive()) {
            deactivate(client);
        } else if (client.options.getCameraType().isFirstPerson()) {
            activateFreecam(client);
        } else {
            activateOrbit(client);
        }
    }

    public static void activateFreecam(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        state.takeSnapshot(player.getEyePosition(), player.getYRot(), player.getXRot());
        state.initState(
            state.getSnapshotX(), state.getSnapshotY(), state.getSnapshotZ(),
            state.getSnapshotYaw(), state.getSnapshotPitch(), 0
        );
        state.setMode(CameraMode.FREECAM);
        state.setTripodMode(false);
        state.setLocked(false);
        state.setActivationCameraType(client.options.getCameraType());

        player.noPhysics = true;
        currentSpeed = BASE_SPEED;
        applyConfigSmoothing();
        displayInfo(client, "Freecam activated");
    }

    public static void activateOrbit(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        state.resetOrbitCenter();

        state.takeSnapshot(player.getEyePosition(), player.getYRot(), player.getXRot());
        state.setTripodMode(false);
        state.setLocked(false);
        state.setActivationCameraType(client.options.getCameraType());

        // If coming from front-facing third-person, start the camera
        // in front of the player instead of behind.
        if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
            OrbitController.resetToFront();
        } else {
            OrbitController.resetToBehind();
        }

        // Compute initial orbit camera position and look-at angles.
        Vec3 camPos = OrbitController.computeCameraPosition(player);
        Vec3 eyePos = player.getEyePosition();
        double lx = eyePos.x - camPos.x;
        double ly = eyePos.y - camPos.y;
        double lz = eyePos.z - camPos.z;
        double h  = Math.sqrt(lx * lx + lz * lz);
        float yaw   = (float) Math.toDegrees(Math.atan2(-lx, lz));
        float pitch = h > 1e-6 ? (float) Math.toDegrees(Math.atan2(-ly, h)) : 0;

        state.initState(camPos.x, camPos.y, camPos.z, yaw, Math.max(-90f, Math.min(90f, pitch)), 0);
        state.setMode(CameraMode.ORBIT);

        applyConfigSmoothing();
        displayInfo(client, "Orbit camera activated");
    }

    public static void deactivate(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        LocalPlayer player = client.player;
        if (player == null) return;

        state.restoreFromSnapshot();
        state.setMode(CameraMode.OFF);
        state.setTripodMode(false);
        state.setLocked(false);
        state.setActivationCameraType(null);
        state.setZooming(false);

        if (player != null) {
            player.noPhysics = false;
        }

        displayInfo(client, "Camera deactivated");
    }

    // ── Reset ─────────────────────────────────────────────────────

    public static void reset(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive() || state.isTripodMode()) return;

        switch (state.getMode()) {
            case FREECAM -> {
                state.restoreFromSnapshot();
                displayInfo(client, "Reset to eye level");
            }
            case ORBIT -> {
                OrbitController.resetToBehind();
                displayInfo(client, "Reset behind player");
            }
        }
    }

    // ── Tripod mode ───────────────────────────────────────────────

    public static void toggleTripod(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) return;

        boolean on = !state.isTripodMode();
        if (on) {
            state.takeLockSnapshot();
        }
        state.setTripodMode(on);
        state.setLocked(on);
        state.setSprinting(false);
        state.setSneaking(false);

        if (client.player != null) {
            // Tripod gives control back to the player (noPhysics = false).
            // In freecam mode, turning tripod OFF restores noClip.
            client.player.noPhysics = (state.getMode() == CameraMode.FREECAM) && !on;
        }

        displayInfo(client, on
            ? "Tripod ON — camera frozen, controls returned to player"
            : "Tripod OFF");
    }

    // ── Per-tick update (20 TPS) ──────────────────────────────────
    // WASD movement and orbit spherical-position are computed in
    // CameraMixin (render loop). This tick handles only smoothing
    // and zoom state.

    public static void tick(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) return;
        if (client.player == null) return;

        if (state.isTripodMode()) {
            client.player.noPhysics = false;
            state.setZooming(false);
            state.tickSmoothing();
            return;
        }

        if (state.getMode() == CameraMode.FREECAM) {
            client.player.noPhysics = true;
        }

        state.tickSmoothing();

        long window = client.getWindow().getWindow();
        boolean vDown = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_V) == GLFW.GLFW_PRESS;
        state.setZooming(vDown);
    }

    // ── Scroll handling ───────────────────────────────────────────

    public static void onScroll(double vertical) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive() || state.isTripodMode()) return;

        switch (state.getMode()) {
            case FREECAM -> {
                float factor = FreeCameraConfig.getInstance().scrollSpeedFactor;
                if (vertical > 0) {
                    currentSpeed = Math.min(currentSpeed * factor, MAX_SPEED);
                } else if (vertical < 0) {
                    currentSpeed = Math.max(currentSpeed / factor, MIN_SPEED);
                }
                if (FreeCameraConfig.getInstance().showSpeedInActionBar) {
                    displaySpeed(Minecraft.getInstance());
                }
            }
            case ORBIT -> OrbitController.onScroll(vertical);
        }
    }

    // ── Cinematic mode ────────────────────────────────────────────

    public static void toggleCinematic(Minecraft client) {
        CameraStateManager state = CameraStateManager.getInstance();
        boolean on = !state.isCinematicMode();
        state.setCinematicMode(on);
        applyConfigSmoothing();
        displayInfo(client, on ? "Cinematic mode ON" : "Cinematic mode OFF");
    }

    // ── Roll (Z / C) — applies in both freecam and orbit ───────────

    public static void roll(float delta) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (state.isLocked()) return;
        state.setCameraRoll(state.getCameraRoll() + delta);
    }

    // ── Config bridge ─────────────────────────────────────────────

    /** Push the config smoothing factor into the state manager. */
    public static void applyConfigSmoothing() {
        CameraStateManager.getInstance().setSmoothingFactor(
            FreeCameraConfig.getInstance().cinematicSmoothingFactor
        );
    }

    // ── UI helpers ────────────────────────────────────────────────

    private static void displayInfo(Minecraft client, String msg) {
        if (client.player == null) return;
        client.player.displayClientMessage(Component.literal(msg), false);
    }

    private static void displaySpeed(Minecraft client) {
        if (client.player == null) return;
        client.player.displayClientMessage(
            Component.literal(String.format("Freecam speed: %.2f", currentSpeed)), false
        );
    }
}
