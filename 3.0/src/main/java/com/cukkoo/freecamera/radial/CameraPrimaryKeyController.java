package com.cukkoo.freecamera.radial;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.lifecycle.ClientIdentitySnapshot;
import com.cukkoo.freecamera.state.CameraStateMachine;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class CameraPrimaryKeyController {
    public static final long HOLD_THRESHOLD_NANOS = 300_000_000L;
    private long holdThresholdNanos = HOLD_THRESHOLD_NANOS;

    public enum KeyDecision { NONE, OPENED, SHORT_PRESS, CONFIRM, CANCEL }
    public enum SelectionAction { ACTIVATE, SWITCH, KEEP_CURRENT }

    private final CameraStateMachine stateMachine;
    private final CameraRadialMenuController radialMenu;
    private CameraMode lastUsedMode = CameraMode.FREE_CAMERA;
    private CameraRadialEntry pendingSelection;
    private long pressedAtNanos;
    private boolean wasDown;
    private boolean cancelledUntilRelease;
    private boolean awaitingNeutralInput;
    private boolean rememberLastUsed=true;private java.util.function.Consumer<CameraMode> rememberedModeSink=mode->{};

    public CameraPrimaryKeyController(
            CameraStateMachine stateMachine,
            CameraRadialMenuController radialMenu
    ) {
        this.stateMachine = stateMachine;
        this.radialMenu = radialMenu;
    }

    public void tick(Minecraft client, KeyMapping primaryKey) {
        boolean quarantineWasActive = awaitingNeutralInput;
        drainClicks(primaryKey);
        KeyDecision decision = updateKeyState(primaryKey.isDown(), System.nanoTime());
        switch (decision) {
            case OPENED -> stateMachine.suspendCameraInput();
            case SHORT_PRESS -> executeShortPress(client);
            case CONFIRM -> executeSelection(client);
            case CANCEL -> beginInputQuarantine();
            case NONE -> { }
        }
        if (quarantineWasActive && awaitingNeutralInput && inputsAreNeutral(client)) {
            awaitingNeutralInput = false;
        }
    }

    public KeyDecision updateKeyState(boolean down, long nowNanos) {
        if (cancelledUntilRelease) {
            if (!down) {
                cancelledUntilRelease = false;
                wasDown = false;
                pressedAtNanos = 0L;
                beginInputQuarantine();
            }
            return KeyDecision.NONE;
        }
        if (down && !wasDown) {
            wasDown = true;
            pressedAtNanos = nowNanos;
            return KeyDecision.NONE;
        }
        if (down) {
            if (!radialMenu.state().isOpen()
                    && nowNanos - pressedAtNanos >= holdThresholdNanos) {
                radialMenu.open();
                return KeyDecision.OPENED;
            }
            return KeyDecision.NONE;
        }
        if (!wasDown) {
            return KeyDecision.NONE;
        }
        long heldNanos = nowNanos - pressedAtNanos;
        wasDown = false;
        pressedAtNanos = 0L;
        if (radialMenu.state().isOpen()) {
            pendingSelection = radialMenu.confirmAndClose();
            beginInputQuarantine();
            return pendingSelection == null ? KeyDecision.CANCEL : KeyDecision.CONFIRM;
        }
        if (heldNanos >= holdThresholdNanos) {
            beginInputQuarantine();
            return KeyDecision.CANCEL;
        }
        return KeyDecision.SHORT_PRESS;
    }

    public void cancelUntilPrimaryRelease() {
        radialMenu.cancelAndClose();
        pendingSelection = null;
        cancelledUntilRelease = wasDown;
        beginInputQuarantine();
    }

    public void rememberSuccessfulMode(CameraMode mode) {
        if (mode != null && rememberLastUsed) {
            lastUsedMode = mode;
            rememberedModeSink.accept(mode);
        }
    }
    public void setHoldDurationMillis(long millis) { holdThresholdNanos=Math.clamp(millis,150,1500)*1_000_000L; }
    public void configurePreferredMode(CameraMode mode,boolean remember,java.util.function.Consumer<CameraMode> sink){lastUsedMode=mode==null?CameraMode.FREE_CAMERA:mode;rememberLastUsed=remember;rememberedModeSink=sink==null?value->{}:sink;}

    public CameraMode lastUsedMode() {
        return lastUsedMode;
    }

    public static SelectionAction resolveSelectionAction(
            CameraMode activeMode,
            CameraMode selectedMode
    ) {
        if (activeMode == null) {
            return SelectionAction.ACTIVATE;
        }
        return activeMode == selectedMode ? SelectionAction.KEEP_CURRENT : SelectionAction.SWITCH;
    }

    public boolean isInputSuppressed() {
        return radialMenu.state().isOpen() || awaitingNeutralInput || cancelledUntilRelease;
    }

    public void clear() {
        radialMenu.clear();
        pendingSelection = null;
        pressedAtNanos = 0L;
        wasDown = false;
        cancelledUntilRelease = false;
        awaitingNeutralInput = false;
    }

    public void drainAndBlockUntilRelease(KeyMapping primaryKey) {
        drainClicks(primaryKey);
        radialMenu.clear();
        pendingSelection = null;
        pressedAtNanos = 0L;
        wasDown = primaryKey.isDown();
        cancelledUntilRelease = primaryKey.isDown();
        awaitingNeutralInput = false;
    }

    private void executeShortPress(Minecraft client) {
        if (stateMachine.isActive()) {
            stateMachine.deactivate(client, CameraStateMachine.CleanupCause.USER_TOGGLE, true);
            beginInputQuarantine();
            return;
        }
        CameraStateMachine.ActivationResult result = stateMachine.requestMode(client, lastUsedMode);
        reportBlockedActivation(client, result);
        if (stateMachine.activeModeOrNull() == lastUsedMode) {
            rememberSuccessfulMode(lastUsedMode);
        }
        beginInputQuarantine();
    }

    private void executeSelection(Minecraft client) {
        CameraRadialEntry selection = pendingSelection;
        pendingSelection = null;
        if (selection == null) {
            beginInputQuarantine();
            return;
        }
        if (stateMachine.isActive()
                && stateMachine.validate(client) != ClientIdentitySnapshot.ValidationResult.VALID) {
            beginInputQuarantine();
            return;
        }
        stateMachine.resumeCameraInput();
        CameraMode selectedMode = selection.mode();
        if (resolveSelectionAction(stateMachine.activeModeOrNull(), selectedMode)
                != SelectionAction.KEEP_CURRENT) {
            CameraStateMachine.ActivationResult result = stateMachine.requestMode(client, selectedMode);
            reportBlockedActivation(client, result);
        }
        if (stateMachine.activeModeOrNull() == selectedMode) {
            rememberSuccessfulMode(selectedMode);
        }
        beginInputQuarantine();
    }

    private void beginInputQuarantine() {
        awaitingNeutralInput = true;
    }

    private static boolean inputsAreNeutral(Minecraft client) {
        return !client.options.keyUp.isDown()
                && !client.options.keyDown.isDown()
                && !client.options.keyLeft.isDown()
                && !client.options.keyRight.isDown()
                && !client.options.keyJump.isDown()
                && !client.options.keyShift.isDown()
                && !client.options.keySprint.isDown()
                && !client.options.keyAttack.isDown()
                && !client.options.keyUse.isDown()
                && !client.options.keyPickItem.isDown();
    }

    private static void reportBlockedActivation(
            Minecraft client,
            CameraStateMachine.ActivationResult result
    ) {
        if (result == CameraStateMachine.ActivationResult.BLOCKED_SPECTATOR) {
            CameraStateMachine.showMessage(client, "message.free-camera.activation_blocked_spectator");
        } else if (result == CameraStateMachine.ActivationResult.BLOCKED_INVALID_WORLD) {
            CameraStateMachine.showMessage(client, "message.free-camera.activation_blocked_world");
        }
    }

    private static void drainClicks(KeyMapping mapping) {
        while (mapping.consumeClick()) { }
    }
}
