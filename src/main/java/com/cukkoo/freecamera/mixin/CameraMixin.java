package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCamera;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public class CameraMixin {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Unique
    private boolean freecam$applyingOffset;

    @Inject(method = "setRotation(FF)V", at = @At("RETURN"))
    private void afterSetRotation(float yaw, float pitch, CallbackInfo ci) {
        if (freecam$applyingOffset || !FreeCamera.isActive()) return;

        freecam$applyingOffset = true;

        this.yRot = FreeCamera.config.cameraYaw;
        this.xRot = Math.clamp(FreeCamera.config.cameraPitch, -90f, 90f);

        freecam$applyingOffset = false;
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("RETURN"), cancellable = true)
    private void onGetMaxZoom(float startDistance, CallbackInfoReturnable<Float> cir) {
        if (!FreeCamera.isActive()) return;
        cir.setReturnValue((float) FreeCamera.config.cameraDistance);
    }
}
