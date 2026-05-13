package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCamera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void onBobView(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
        if (FreeCamera.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void onBobHurt(CameraRenderState state, PoseStack pose, CallbackInfo ci) {
        if (FreeCamera.isActive()) {
            ci.cancel();
        }
    }
}
