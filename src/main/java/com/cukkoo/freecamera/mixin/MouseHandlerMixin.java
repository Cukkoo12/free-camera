package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCamera;
import com.cukkoo.freecamera.config.FreeCameraConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer(D)V", at = @At("HEAD"), cancellable = true)
    private void onTurnPlayer(double sensitivity, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!FreeCamera.isActive() || client.player == null || client.options.getCameraType().isFirstPerson()) {
            return;
        }

        FreeCameraConfig cfg = FreeCamera.config;

        float dx = (float) (accumulatedDX * sensitivity * cfg.rotationSensitivity);
        float dy = (float) (accumulatedDY * sensitivity * cfg.rotationSensitivity);

        cfg.cameraYaw += dx;
        cfg.cameraPitch += dy;

        accumulatedDX = 0;
        accumulatedDY = 0;
        ci.cancel();
    }

    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        if (!FreeCamera.isActive() || client.player == null || client.screen != null
                || client.options.getCameraType().isFirstPerson()) {
            return;
        }

        FreeCameraConfig cfg = FreeCamera.config;
        cfg.cameraDistance = Math.clamp(cfg.cameraDistance - vertical * 0.5, 1.0, 20.0);
        cfg.save();

        ci.cancel();
    }
}
