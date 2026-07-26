package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class ViewBobbingMixin {
    @Inject(
            method = "bobView(Lnet/minecraft/client/renderer/state/level/CameraRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void freecamera$suppressWalkingViewBobbing(
            CameraRenderState cameraState,
            PoseStack poseStack,
            CallbackInfo ci
    ) {
        CameraStateMachine stateMachine = FreeCameraClient.stateMachineOrNull();
        if (stateMachine == null || !stateMachine.isActive() || !FreeCameraClient.suppressViewBobbingEnabled()) {
            return;
        }
        ci.cancel();
    }
}
