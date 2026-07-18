package com.cukkoo.freecamera.state;

import net.minecraft.client.CameraType;
import net.minecraft.world.phys.Vec3;

/**
 * Central camera state manager.
 *
 * <p>Current values are read every render frame by CameraMixin.
 * Target values are written by controllers.
 * {@link #tickSmoothing()} lerps current toward target at 20 TPS
 * when cinematic mode is on.</p>
 */
public final class CameraStateManager {

    private static final CameraStateManager INSTANCE = new CameraStateManager();

    public static CameraStateManager getInstance() {
        return INSTANCE;
    }

    // ── Mode ──────────────────────────────────────────────────────
    private CameraMode mode = CameraMode.OFF;

    // ── Current (rendered) values ─────────────────────────────────
    private double currentX;
    private double currentY;
    private double currentZ;
    private float currentYaw;
    private float currentPitch;
    private float currentRoll;

    // ── Target values (written by controllers) ────────────────────
    private double targetX;
    private double targetY;
    private double targetZ;
    private float targetYaw;
    private float targetPitch;

    // ── Previous-frame values ─────────────────────────────────────
    private double prevX;
    private double prevY;
    private double prevZ;
    private float prevYaw;
    private float prevPitch;

    // ── Snapshot of original player values at activation ──────────
    private boolean snapshotTaken;
    private double snapshotX;
    private double snapshotY;
    private double snapshotZ;
    private float snapshotYaw;
    private float snapshotPitch;

    // ── Movement state ────────────────────────────────────────────
    private boolean sprinting;
    private boolean sneaking;

    // ── Cinematic smoothing ───────────────────────────────────────
    private boolean cinematicMode;
    private float smoothingFactor = 0.12f;

    // ── Tripod (camera freeze) ────────────────────────────────────
    private boolean tripodMode;

    // ── Locked (controls returned to player, camera frozen) ──────
    private boolean locked;

    // ── F7 snapshot lock ──────────────────────────────────────────
    private double lockedX;
    private double lockedY;
    private double lockedZ;
    private float lockedYaw;
    private float lockedPitch;
    private float lockedRoll;

    // ── Camera type at activation (for F5 auto-deactivation) ──
    private CameraType activationCameraType;

    // ── Zoom ──────────────────────────────────────────────────────
    private boolean zooming;

    private CameraStateManager() {
        reset();
    }

    // ── Lifecycle ─────────────────────────────────────────────────

    public void reset() {
        this.mode = CameraMode.OFF;
        this.currentX = 0;    this.targetX = 0;
        this.currentY = 0;    this.targetY = 0;
        this.currentZ = 0;    this.targetZ = 0;
        this.currentYaw = 0;  this.targetYaw = 0;
        this.currentPitch = 0; this.targetPitch = 0;
        this.currentRoll = 0;
        this.prevX = 0; this.prevY = 0; this.prevZ = 0;
        this.prevYaw = 0; this.prevPitch = 0;
        this.snapshotTaken = false;
        this.snapshotX = 0; this.snapshotY = 0; this.snapshotZ = 0;
        this.snapshotYaw = 0; this.snapshotPitch = 0;
        this.sprinting = false; this.sneaking = false;
        this.cinematicMode = false;
        this.smoothingFactor = 0.12f;
        this.tripodMode = false;
        this.locked = false;
        this.lockedX = 0; this.lockedY = 0; this.lockedZ = 0;
        this.lockedYaw = 0; this.lockedPitch = 0; this.lockedRoll = 0;
        this.zooming = false;
        this.orbitCenterInitialized = false;
    }

    public void takeSnapshot(Vec3 pos, float yaw, float pitch) {
        this.snapshotX = pos.x;
        this.snapshotY = pos.y;
        this.snapshotZ = pos.z;
        this.snapshotYaw = yaw;
        this.snapshotPitch = pitch;
        this.snapshotTaken = true;
    }

    public void restoreFromSnapshot() {
        if (!snapshotTaken) return;
        setCameraPosition(snapshotX, snapshotY, snapshotZ);
        setCameraYaw(snapshotYaw);
        setCameraPitch(snapshotPitch);
        setCameraRoll(0);
    }

    /** Force-set prev=current=target at activation. */
    public void initState(double x, double y, double z, float yaw, float pitch, float roll) {
        this.prevX = this.currentX = this.targetX = x;
        this.prevY = this.currentY = this.targetY = y;
        this.prevZ = this.currentZ = this.targetZ = z;
        this.prevYaw = this.currentYaw = this.targetYaw = yaw;
        this.prevPitch = this.currentPitch = this.targetPitch = pitch;
        this.currentRoll = roll;
    }

    // ── Smoothing ─────────────────────────────────────────────────

    /** Called each client tick (20 TPS). Lerps current toward target
     *  when cinematic mode is on. Skipped when locked. */
    public void tickSmoothing() {
        if (!isActive() || isLocked()) return;

        if (cinematicMode) {
            currentX       = lerp(currentX,       targetX,       smoothingFactor);
            currentY       = lerp(currentY,       targetY,       smoothingFactor);
            currentZ       = lerp(currentZ,       targetZ,       smoothingFactor);
            currentYaw     = lerpAngle(currentYaw,   targetYaw,   smoothingFactor);
            currentPitch   = lerpAngle(currentPitch, targetPitch, smoothingFactor);
            currentRoll    = lerpAngle(currentRoll,  targetRoll(), smoothingFactor);
        } else {
            currentX   = targetX;   currentYaw   = targetYaw;
            currentY   = targetY;   currentPitch = targetPitch;
            currentZ   = targetZ;   currentRoll  = targetRoll();
        }
    }

    // ── Getters (always return CURRENT / rendered values) ─────────

    public CameraMode getMode()               { return mode; }
    public boolean   isActive()               { return mode != CameraMode.OFF; }

    public double getCameraX()                { return currentX; }
    public double getCameraY()                { return currentY; }
    public double getCameraZ()                { return currentZ; }
    public Vec3 getCameraPosition()           { return new Vec3(currentX, currentY, currentZ); }
    public float getCameraYaw()               { return currentYaw; }
    public float getCameraPitch()             { return currentPitch; }
    public float getCameraRoll()              { return currentRoll; }

    public boolean isCinematicMode()          { return cinematicMode; }
    public boolean isTripodMode()             { return tripodMode; }
    public boolean isLocked()                 { return locked; }
    public boolean isZooming()                { return zooming; }

    // ── Locked snapshot (F7 tripod) ───────────────────────────────

    public void takeLockSnapshot() {
        this.lockedX = this.currentX;
        this.lockedY = this.currentY;
        this.lockedZ = this.currentZ;
        this.lockedYaw = this.currentYaw;
        this.lockedPitch = this.currentPitch;
        this.lockedRoll = this.currentRoll;
    }

    public double getLockedX()                { return lockedX; }
    public double getLockedY()                { return lockedY; }
    public double getLockedZ()                { return lockedZ; }
    public float  getLockedYaw()              { return lockedYaw; }
    public float  getLockedPitch()            { return lockedPitch; }
    public float  getLockedRoll()             { return lockedRoll; }

    // ── Setters ──────────────────────────────────────────────────

    public void setMode(CameraMode mode)      { this.mode = mode; }

    public void setCameraPosition(double x, double y, double z) {
        this.targetX = x; this.targetY = y; this.targetZ = z;
        if (!cinematicMode) {
            this.currentX = x; this.currentY = y; this.currentZ = z;
        }
    }

    /** Update only current position (used by orbit mode to sync vanilla camera pos). */
    public void syncCurrentPosition(double x, double y, double z) {
        this.currentX = x;
        this.currentY = y;
        this.currentZ = z;
    }

    /** Update only current yaw (used by orbit mode for lock snapshot). */
    public void syncCurrentYaw(float yaw) {
        this.currentYaw = yaw;
    }

    /** Update only current pitch (used by orbit mode for lock snapshot). */
    public void syncCurrentPitch(float pitch) {
        this.currentPitch = pitch;
    }

    public void setCameraYaw(float yaw) {
        this.targetYaw = yaw;
        if (!cinematicMode) this.currentYaw = yaw;
    }

    public void setCameraPitch(float pitch) {
        this.targetPitch = pitch;
        if (!cinematicMode) this.currentPitch = pitch;
    }

    public void setCameraRoll(float roll) {
        this.currentRoll = roll;
    }

    public float getSmoothingFactor() {
        return smoothingFactor;
    }

    public void setSmoothingFactor(float factor) {
        this.smoothingFactor = Math.max(0.01f, Math.min(0.5f, factor));
    }

    // ── Mode toggles ──────────────────────────────────────────────

    public void setCinematicMode(boolean cinematicMode) {
        if (cinematicMode && !this.cinematicMode) {
            currentX   = targetX;   currentYaw   = targetYaw;
            currentY   = targetY;   currentPitch = targetPitch;
            currentZ   = targetZ;   currentRoll  = targetRoll();
        }
        this.cinematicMode = cinematicMode;
    }

    public void setTripodMode(boolean tripodMode) {
        this.tripodMode = tripodMode;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public CameraType getActivationCameraType() {
        return activationCameraType;
    }

    public void setActivationCameraType(CameraType activationCameraType) {
        this.activationCameraType = activationCameraType;
    }

    public void setZooming(boolean zooming)   { this.zooming = zooming; }

    // ── Snapshot getters ──────────────────────────────────────────

    public boolean isSnapshotTaken()          { return snapshotTaken; }
    public double getSnapshotX()              { return snapshotX; }
    public double getSnapshotY()              { return snapshotY; }
    public double getSnapshotZ()              { return snapshotZ; }
    public float  getSnapshotYaw()            { return snapshotYaw; }
    public float  getSnapshotPitch()          { return snapshotPitch; }

    // ── Movement input ────────────────────────────────────────────

    public boolean isSprinting()              { return sprinting; }
    public void  setSprinting(boolean v)      { this.sprinting = v; }
    public boolean isSneaking()               { return sneaking; }
    public void  setSneaking(boolean v)       { this.sneaking = v; }

    // ── Orbit center smoothing ─────────────────────────────────────

    private double orbitCenterX, orbitCenterY, orbitCenterZ;
    private boolean orbitCenterInitialized;

    public double getOrbitCenterX() { return orbitCenterX; }
    public double getOrbitCenterY() { return orbitCenterY; }
    public double getOrbitCenterZ() { return orbitCenterZ; }

    public void tickOrbitCenter(double targetX, double targetY, double targetZ, float alpha) {
        if (!orbitCenterInitialized) {
            orbitCenterX = targetX;
            orbitCenterY = targetY;
            orbitCenterZ = targetZ;
            orbitCenterInitialized = true;
        } else {
            double dx = targetX - orbitCenterX;
            double dy = targetY - orbitCenterY;
            double dz = targetZ - orbitCenterZ;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > 100.0) {
                // Teleport detection — snap
                orbitCenterX = targetX;
                orbitCenterY = targetY;
                orbitCenterZ = targetZ;
            } else {
                orbitCenterX += (targetX - orbitCenterX) * alpha;
                orbitCenterY += (targetY - orbitCenterY) * alpha;
                orbitCenterZ += (targetZ - orbitCenterZ) * alpha;
            }
        }
    }

    public void resetOrbitCenter() {
        orbitCenterInitialized = false;
    }

    // ── Internal helpers ──────────────────────────────────────────

    private float targetRoll() {
        return currentRoll;
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private static float lerpAngle(float a, float b, float t) {
        float diff = (b - a) % 360;
        if (diff > 180)  diff -= 360;
        if (diff < -180) diff += 360;
        return a + diff * t;
    }
}
