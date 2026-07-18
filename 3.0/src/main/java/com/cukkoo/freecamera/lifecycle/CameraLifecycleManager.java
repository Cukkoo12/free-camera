package com.cukkoo.freecamera.lifecycle;

import com.cukkoo.freecamera.input.CameraKeyMappings;
import com.cukkoo.freecamera.state.CameraStateMachine;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

public final class CameraLifecycleManager {
    private final CameraStateMachine stateMachine;
    private final CameraKeyMappings keyMappings;

    public CameraLifecycleManager(CameraStateMachine stateMachine, CameraKeyMappings keyMappings) {
        this.stateMachine = stateMachine;
        this.keyMappings = keyMappings;
    }

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                forceCleanup(client, CameraStateMachine.CleanupCause.DISCONNECTED));
        ClientLifecycleEvents.CLIENT_STOPPING.register(client ->
                forceCleanup(client, CameraStateMachine.CleanupCause.CLIENT_SHUTDOWN));
    }

    private void onEndClientTick(Minecraft client) {
        stateMachine.beginClientTick();
        if (stateMachine.isActive()) {
            ClientIdentitySnapshot.ValidationResult result = stateMachine.validate(client);
            if (result != ClientIdentitySnapshot.ValidationResult.VALID) {
                forceCleanup(client, cleanupCause(result));
            }
        }

        if (client.screen != null) {
            keyMappings.forceClearRadial();
            stateMachine.suspendCameraInput();
            keyMappings.clearQueuedTransitions();
            return;
        }
        if (keyMappings.isRadialInputSuppressed()) {
            stateMachine.suspendCameraInput();
        } else if (stateMachine.isActive()) {
            stateMachine.resumeCameraInput();
        }
        keyMappings.processTick(client);
        if (keyMappings.isRadialInputSuppressed()) {
            stateMachine.suspendCameraInput();
        }
    }

    private void forceCleanup(Minecraft client, CameraStateMachine.CleanupCause cause) {
        keyMappings.forceClearRadial();
        if (stateMachine.isActive()) {
            stateMachine.deactivate(client, cause, false);
        }
        keyMappings.clearQueuedTransitions();
    }

    private static CameraStateMachine.CleanupCause cleanupCause(
            ClientIdentitySnapshot.ValidationResult result
    ) {
        return switch (result) {
            case LEVEL_MISSING -> CameraStateMachine.CleanupCause.LEVEL_MISSING;
            case PLAYER_MISSING -> CameraStateMachine.CleanupCause.PLAYER_MISSING;
            case CONNECTION_MISSING -> CameraStateMachine.CleanupCause.CONNECTION_MISSING;
            case LEVEL_REPLACED -> CameraStateMachine.CleanupCause.LEVEL_REPLACED;
            case PLAYER_REPLACED -> CameraStateMachine.CleanupCause.PLAYER_REPLACED;
            case PLAYER_UUID_CHANGED -> CameraStateMachine.CleanupCause.PLAYER_UUID_CHANGED;
            case CONNECTION_REPLACED -> CameraStateMachine.CleanupCause.CONNECTION_REPLACED;
            case DIMENSION_CHANGED -> CameraStateMachine.CleanupCause.DIMENSION_CHANGED;
            case PLAYER_DIED -> CameraStateMachine.CleanupCause.PLAYER_DIED;
            case SPECTATOR_ENTERED -> CameraStateMachine.CleanupCause.SPECTATOR_ENTERED;
            case PERSPECTIVE_CHANGED -> CameraStateMachine.CleanupCause.PERSPECTIVE_CHANGED;
            case VALID -> throw new IllegalArgumentException("A valid session does not need cleanup");
        };
    }
}
