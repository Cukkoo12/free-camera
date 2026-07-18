package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.cukkoo.freecamera.state.CameraStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(PoseStack poseStack, float tickDelta, CallbackInfo ci) {
        if (CameraStateManager.getInstance().isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onBobHurt(PoseStack poseStack, float tickDelta, CallbackInfo ci) {
        if (CameraStateManager.getInstance().isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void onGetFov(Camera camera, float tickDelta, boolean b, CallbackInfoReturnable<Double> cir) {
        if (CameraStateManager.getInstance().isZooming()) {
            cir.setReturnValue(cir.getReturnValue() * FreeCameraConfig.getInstance().zoomFactor);
        }
    }
}
