package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.render.DetachedCameraRenderPolicy;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class HandRenderingMixin {
    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void freecamera$hideDetachedCameraHand(
            CameraRenderState cameraState,
            float partialTick,
            Matrix4fc projectionMatrix,
            CallbackInfo ci
    ) {
        CameraStateMachine stateMachine = FreeCameraClient.stateMachineOrNull();
        if (stateMachine == null
                || !stateMachine.isActive()
                || !FreeCameraClient.hideHandEnabled()
                || !DetachedCameraRenderPolicy.hideHand(stateMachine.activeModeOrNull())) {
            return;
        }
        ci.cancel();
    }
}
