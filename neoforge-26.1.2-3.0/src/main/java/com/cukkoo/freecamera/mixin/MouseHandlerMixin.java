package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.state.CameraStateMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void freecamera$divertMouseLook(double frameDelta, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (FreeCameraClient.isRadialInputSuppressed()) {
            if (FreeCameraClient.isRadialMenuOpen()) {
                FreeCameraClient.accumulateRadialMouse(accumulatedDX, accumulatedDY);
            }
            accumulatedDX = 0.0D;
            accumulatedDY = 0.0D;
            ci.cancel();
            return;
        }
        CameraStateMachine stateMachine = FreeCameraClient.stateMachineOrNull();
        if (stateMachine == null || !stateMachine.shouldInterceptCameraMouseInput()
                || client.screen != null) {
            return;
        }

        double sensitivity = client.options.sensitivity().get() * 0.6000000238418579D
                + 0.20000000298023224D;
        double sensitivityScale = sensitivity * sensitivity * sensitivity * 8.0D;
        double yawInput = accumulatedDX * sensitivityScale * FreeCameraClient.cameraMouseSensitivity();
        double pitchInput = accumulatedDY * sensitivityScale * FreeCameraClient.cameraMouseSensitivity();

        if (client.options.invertMouseX().get()) {
            yawInput = -yawInput;
        }
        if (client.options.invertMouseY().get()) {
            pitchInput = -pitchInput;
        }

        stateMachine.rotateCamera(client, yawInput, pitchInput);
        accumulatedDX = 0.0D;
        accumulatedDY = 0.0D;
        ci.cancel();
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void freecamera$changeSpeedPreset(
            long window,
            double horizontal,
            double vertical,
            CallbackInfo ci
    ) {
        if (FreeCameraClient.isRadialInputSuppressed()) {
            ci.cancel();
            return;
        }
        if (FreeCameraClient.handleSpeedPresetScroll(window, vertical)) {
            ci.cancel();
        }
    }

    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void freecamera$suppressRadialMouseButtons(
            long window,
            MouseButtonInfo button,
            int action,
            CallbackInfo ci
    ) {
        if (!FreeCameraClient.isRadialInputSuppressed()) {
            return;
        }
        if (FreeCameraClient.isRadialMenuOpen() && button.button() == 1 && action == 1) {
            FreeCameraClient.cancelRadialMenu();
        }
        ci.cancel();
    }
}
