package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.render.CameraRenderEffectsBridge;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraRenderEffectsMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void freecamera$applyZoomProjection(
            CameraRenderState cameraState,
            float partialTick,
            CallbackInfo ci
    ) {
        CameraRenderEffectsBridge bridge = FreeCameraClient.renderEffectsBridgeOrNull();
        if (bridge != null) {
            bridge.applyProjection(cameraState);
        }
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void freecamera$prepareCanonicalWorldView(
            net.minecraft.client.DeltaTracker deltaTracker,
            CallbackInfo ci
    ) {
        CameraRenderEffectsBridge bridge = FreeCameraClient.renderEffectsBridgeOrNull();
        if (bridge != null) {
            bridge.prepareFrame(Minecraft.getInstance(), (Camera) (Object) this);
        }
    }
}
