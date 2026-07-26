package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.state.CameraStateMachine;
import com.cukkoo.freecamera.render.DetachedCameraRenderPolicy;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class CrosshairRenderingMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void freecamera$hideDetachedCameraCrosshair(
            GuiGraphicsExtractor graphics,
            DeltaTracker tracker,
            CallbackInfo ci
    ) {
        CameraStateMachine stateMachine = FreeCameraClient.stateMachineOrNull();
        if (stateMachine == null
                || !stateMachine.isActive()
                || !FreeCameraClient.hideCrosshairEnabled()
                || !DetachedCameraRenderPolicy.hideCrosshair(stateMachine.activeModeOrNull())) {
            return;
        }
        ci.cancel();
    }
}
