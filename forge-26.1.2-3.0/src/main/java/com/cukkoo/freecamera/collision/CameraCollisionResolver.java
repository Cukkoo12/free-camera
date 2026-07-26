package com.cukkoo.freecamera.collision;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.state.CameraStateMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class CameraCollisionResolver {
    private static final double POSITION_EPSILON_SQUARED = 1.0E-12;

    private final CameraCollisionState state = new CameraCollisionState();
    private final FreeCameraCollisionResolver freeResolver = new FreeCameraCollisionResolver();
    private final OrbitCameraCollisionResolver orbitResolver = new OrbitCameraCollisionResolver();
    private final CameraCollisionSmoother smoother = new CameraCollisionSmoother();
    private ClientLevel cachedLevel;
    private MinecraftCameraCollisionQuery cachedQuery;
    private boolean worldCollisionDirty = true;

    public CameraCollisionState state() {
        return state;
    }
    public void configure(double safetyMargin){freeResolver.configure(safetyMargin);orbitResolver.configure(safetyMargin);}

    public void resolve(
            Minecraft client,
            CameraStateMachine stateMachine,
            CameraMode mode,
            CameraPose desired,
            CameraPose rendered,
            double anchorX,
            double anchorY,
            double anchorZ
    ) {
        CameraCollisionMode collisionMode = state.modeFor(mode);
        if (mode == CameraMode.TRIPOD || collisionMode == CameraCollisionMode.OFF) {
            resolveUnrestricted(mode, desired, rendered);
            return;
        }
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return;
        }
        if (!stateMachine.isCameraInputEnabled()) {
            suspend();
            return;
        }
        double elapsed = state.elapsedSeconds(System.nanoTime());
        if (cachedLevel != level) {
            cachedLevel = level;
            cachedQuery = new MinecraftCameraCollisionQuery(level);
            worldCollisionDirty = true;
        }
        resolveInternal(
                cachedQuery,
                mode,
                collisionMode,
                desired,
                rendered,
                anchorX,
                anchorY,
                anchorZ,
                elapsed
        );
        worldCollisionDirty = false;
    }

    /** Resolves a playback desired pose without changing the configured mode or saved track. */
    public void resolvePlayback(Minecraft client, CameraStateMachine stateMachine,
                                CameraCollisionMode collisionMode, CameraPose desired, CameraPose rendered) {
        if (collisionMode == CameraCollisionMode.OFF) { rendered.copyFrom(desired); return; }
        if (client.level == null || client.player == null || !stateMachine.isCameraInputEnabled()) return;
        if (cachedLevel != client.level) { cachedLevel=client.level; cachedQuery=new MinecraftCameraCollisionQuery(client.level); worldCollisionDirty=true; }
        resolveInternal(cachedQuery, CameraMode.FREE_CAMERA, collisionMode, desired, rendered,
                desired.x(), desired.y(), desired.z(), state.elapsedSeconds(System.nanoTime()));
        worldCollisionDirty=false;
    }

    void resolveForDevelopmentCheck(
            CameraCollisionQuery query,
            CameraMode mode,
            CameraCollisionMode collisionMode,
            CameraPose desired,
            CameraPose rendered,
            double anchorX,
            double anchorY,
            double anchorZ,
            double elapsed
    ) {
        resolveInternal(
                query, mode, collisionMode, desired, rendered,
                anchorX, anchorY, anchorZ, elapsed
        );
        worldCollisionDirty = false;
    }

    public void suspend() {
        state.suspend();
    }

    public void clear() {
        state.clear();
        cachedLevel = null;
        cachedQuery = null;
        worldCollisionDirty = true;
    }

    public void markWorldCollisionDirty() {
        worldCollisionDirty = true;
    }

    private void resolveInternal(
            CameraCollisionQuery query,
            CameraMode mode,
            CameraCollisionMode collisionMode,
            CameraPose desired,
            CameraPose rendered,
            double anchorX,
            double anchorY,
            double anchorZ,
            double elapsed
    ) {
        if (!state.isInitializedFor(mode)) {
            state.seed(mode, rendered.x(), rendered.y(), rendered.z());
        }
        if (mode == CameraMode.TRIPOD || collisionMode == CameraCollisionMode.OFF) {
            resolveUnrestricted(mode, desired, rendered);
            return;
        }

        if (mode == CameraMode.ORBIT || mode == CameraMode.FOLLOW) {
            resolveOrbit(
                    query, collisionMode, desired, rendered,
                    anchorX, anchorY, anchorZ, elapsed
            );
        } else {
            resolveFree(query, collisionMode, desired, rendered, elapsed);
        }
        rendered.setRotation(desired.yaw(), desired.pitch());
    }

    private void resolveFree(
            CameraCollisionQuery query,
            CameraCollisionMode collisionMode,
            CameraPose desired,
            CameraPose rendered,
            double elapsed
    ) {
        double fromX = state.resolvedX();
        double fromY = state.resolvedY();
        double fromZ = state.resolvedZ();
        if (!worldCollisionDirty
                && distanceSquared(fromX, fromY, fromZ, desired.x(), desired.y(), desired.z())
                <= POSITION_EPSILON_SQUARED) {
            rendered.setPosition(fromX, fromY, fromZ);
            return;
        }

        boolean currentFree = query.isSweepLoaded(
                fromX, fromY, fromZ, fromX, fromY, fromZ,
                FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
        ) && query.isVolumeFree(
                fromX, fromY, fromZ, FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
        );
        if (!currentFree) {
            if (state.hasSafePosition() && query.isVolumeFree(
                    state.safeX(), state.safeY(), state.safeZ(),
                    FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
            )) {
                fromX = state.safeX();
                fromY = state.safeY();
                fromZ = state.safeZ();
            } else if (state.hasPreviousSafePosition() && query.isVolumeFree(
                    state.previousSafeX(), state.previousSafeY(), state.previousSafeZ(),
                    FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
            )) {
                fromX = state.previousSafeX();
                fromY = state.previousSafeY();
                fromZ = state.previousSafeZ();
            } else if (query.isVolumeFree(
                    desired.x(), desired.y(), desired.z(),
                    FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
            )) {
                updateFreeResult(query, desired, rendered, desired.x(), desired.y(), desired.z(), false);
                return;
            } else {
                rendered.setPosition(fromX, fromY, fromZ);
                state.updateResolved(fromX, fromY, fromZ, true);
                return;
            }
        }

        CameraCollisionResult hard = freeResolver.resolve(
                query,
                fromX, fromY, fromZ,
                desired.x(), desired.y(), desired.z()
        );
        if (hard.unloadedBoundary) {
            rendered.setPosition(fromX, fromY, fromZ);
            state.updateResolved(fromX, fromY, fromZ, true);
            return;
        }

        double resultX = hard.x;
        double resultY = hard.y;
        double resultZ = hard.z;
        if (collisionMode == CameraCollisionMode.SOFT && state.isObstructed() && !hard.obstructed) {
            double fraction = smoother.recoveryFraction(elapsed);
            resultX = fromX + (desired.x() - fromX) * fraction;
            resultY = fromY + (desired.y() - fromY) * fraction;
            resultZ = fromZ + (desired.z() - fromZ) * fraction;
        }
        updateFreeResult(query, desired, rendered, resultX, resultY, resultZ, hard.obstructed);
    }

    private void updateFreeResult(
            CameraCollisionQuery query,
            CameraPose desired,
            CameraPose rendered,
            double x,
            double y,
            double z,
            boolean obstructed
    ) {
        rendered.setPosition(x, y, z);
        state.updateResolved(x, y, z, obstructed);
        state.rememberQuery(desired.x(), desired.y(), desired.z(), Double.NaN, Double.NaN, Double.NaN);
        if (query.isVolumeFree(x, y, z, FreeCameraCollisionResolver.CAMERA_HALF_EXTENT)) {
            state.rememberSafe(x, y, z);
        }
    }

    private void resolveOrbit(
            CameraCollisionQuery query,
            CameraCollisionMode collisionMode,
            CameraPose desired,
            CameraPose rendered,
            double anchorX,
            double anchorY,
            double anchorZ,
            double elapsed
    ) {
        boolean unchanged = state.desiredAndAnchorUnchanged(
                desired.x(), desired.y(), desired.z(), anchorX, anchorY, anchorZ
        );
        if (!worldCollisionDirty && unchanged && !state.isObstructed()) {
            rendered.setPosition(state.resolvedX(), state.resolvedY(), state.resolvedZ());
            return;
        }
        CameraCollisionResult target = orbitResolver.resolve(
                query,
                anchorX, anchorY, anchorZ,
                desired.x(), desired.y(), desired.z()
        );
        if (target.unloadedBoundary) {
            rendered.setPosition(state.resolvedX(), state.resolvedY(), state.resolvedZ());
            state.updateResolved(
                    state.resolvedX(), state.resolvedY(), state.resolvedZ(), true
            );
            return;
        }

        double resultX = target.x;
        double resultY = target.y;
        double resultZ = target.z;
        double currentDistanceSquared = distanceSquared(
                anchorX, anchorY, anchorZ,
                state.resolvedX(), state.resolvedY(), state.resolvedZ()
        );
        double targetDistanceSquared = distanceSquared(
                anchorX, anchorY, anchorZ,
                target.x, target.y, target.z
        );
        if (collisionMode == CameraCollisionMode.SOFT
                && state.isObstructed()
                && targetDistanceSquared > currentDistanceSquared) {
            double fraction = smoother.recoveryFraction(elapsed);
            double currentDistance = Math.sqrt(currentDistanceSquared);
            double targetDistance = Math.sqrt(targetDistanceSquared);
            double recoveredDistance = currentDistance
                    + (targetDistance - currentDistance) * fraction;
            double radialScale = targetDistance <= 1.0E-9
                    ? 0.0
                    : recoveredDistance / targetDistance;
            resultX = anchorX + (target.x - anchorX) * radialScale;
            resultY = anchorY + (target.y - anchorY) * radialScale;
            resultZ = anchorZ + (target.z - anchorZ) * radialScale;
        }
        rendered.setPosition(resultX, resultY, resultZ);
        boolean stillRecovering = distanceSquared(
                resultX, resultY, resultZ, target.x, target.y, target.z
        ) > POSITION_EPSILON_SQUARED;
        state.updateResolved(resultX, resultY, resultZ, target.obstructed || stillRecovering);
        state.rememberQuery(
                desired.x(), desired.y(), desired.z(), anchorX, anchorY, anchorZ
        );
        if (query.isVolumeFree(
                resultX, resultY, resultZ, OrbitCameraCollisionResolver.CAMERA_HALF_EXTENT
        )) {
            state.rememberSafe(resultX, resultY, resultZ);
        }
    }

    private static double distanceSquared(
            double firstX, double firstY, double firstZ,
            double secondX, double secondY, double secondZ
    ) {
        double x = firstX - secondX;
        double y = firstY - secondY;
        double z = firstZ - secondZ;
        return x * x + y * y + z * z;
    }

    private void resolveUnrestricted(
            CameraMode mode,
            CameraPose desired,
            CameraPose rendered
    ) {
        if (!state.isInitializedFor(mode)) {
            state.seed(mode, rendered.x(), rendered.y(), rendered.z());
        }
        rendered.copyFrom(desired);
        state.updateResolved(desired.x(), desired.y(), desired.z(), false);
        state.rememberQuery(
                desired.x(), desired.y(), desired.z(),
                Double.NaN, Double.NaN, Double.NaN
        );
    }
}
