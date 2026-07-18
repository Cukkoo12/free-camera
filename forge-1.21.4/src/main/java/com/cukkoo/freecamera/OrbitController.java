package com.cukkoo.freecamera;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Controls the third-person orbit camera using spherical coordinates.
 * Angles are updated by mouse input; the actual camera position
 * is calculated every render frame inside {@link
 * com.cukkoo.freecamera.mixin.CameraMixin} for smooth, frame-rate-
 * independent motion.
 */
public final class OrbitController {

    private static final float MIN_RADIUS    = 1.5f;
    private static final float MAX_RADIUS    = 20.0f;
    private static final float RADIUS_SCROLL = 1.2f;
    private static final float BASE_RADIUS   = 5.0f;
    private static final float BASE_PHI      = 15.0f;

    // Spherical coordinates (degrees)
    private static float orbitTheta;   // horizontal angle around player
    private static float orbitPhi;     // vertical angle
    private static float orbitRadius;

    private OrbitController() { }

    // ── Initialisation ────────────────────────────────────────────

    public static void resetToBehind() {
        orbitTheta = 0;
        orbitPhi   = BASE_PHI;
        orbitRadius = BASE_RADIUS;
    }

    /** Called when activating orbit from front-facing third-person. */
    public static void resetToFront() {
        orbitTheta = 180;
        orbitPhi   = BASE_PHI;
        orbitRadius = BASE_RADIUS;
    }

    // ── Mouse input ───────────────────────────────────────────────

    public static void onMouseMove(double deltaX, double deltaY) {
        FreeCameraConfig cfg = FreeCameraConfig.getInstance();
        float sens = cfg.orbitSensitivity;
        float yMul = cfg.invertOrbitY ? -1 : 1;

        orbitTheta += (float) deltaX * sens;
        orbitPhi    = Math.clamp(orbitPhi + (float) deltaY * sens * yMul, -89.0f, 89.0f);
    }

    public static void onScroll(double vertical) {
        if (vertical > 0) {
            orbitRadius = Math.min(orbitRadius * RADIUS_SCROLL, MAX_RADIUS);
        } else if (vertical < 0) {
            orbitRadius = Math.max(orbitRadius / RADIUS_SCROLL, MIN_RADIUS);
        }
    }

    // ── Getters (called from CameraMixin) ─────────────────────────

    public static float getTheta()  { return orbitTheta; }
    public static float getPhi()    { return orbitPhi; }
    public static float getRadius() { return orbitRadius; }

    /** Compute the world-space camera position from orbit parameters. */
    public static Vec3 computeCameraPosition(LocalPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        float thetaRad = (float) Math.toRadians(orbitTheta);
        float phiRad   = (float) Math.toRadians(orbitPhi);
        float radius   = orbitRadius;

        float pYawRad = player.getYRot() * (float) Math.PI / 180.0f;
        double behindX = Math.sin(pYawRad);
        double behindZ = -Math.cos(pYawRad);
        float cosT = (float) Math.cos(thetaRad);
        float sinT = (float) Math.sin(thetaRad);
        double dirX = behindX * cosT - behindZ * sinT;
        double dirZ = behindX * sinT + behindZ * cosT;

        return new Vec3(
            eyePos.x + radius * Math.cos(phiRad) * dirX,
            eyePos.y + radius * Math.sin(phiRad),
            eyePos.z + radius * Math.cos(phiRad) * dirZ
        );
    }
}
