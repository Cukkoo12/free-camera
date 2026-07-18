package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.render.CameraRenderBridge;
import com.cukkoo.freecamera.render.DetachedCameraFov;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow
    private boolean detached;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow
    protected abstract void setRotation(float yaw, float pitch);

    @Inject(
            method = "update(Lnet/minecraft/client/DeltaTracker;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/Camera;calculateFov(F)F"
            )
    )
    private void freecamera$applyStationaryPose(DeltaTracker tracker, CallbackInfo ci) {
        CameraRenderBridge bridge = FreeCameraClient.renderBridgeOrNull();
        if (bridge == null) {
            return;
        }
        CameraPose pose = bridge.activePoseOrNull(Minecraft.getInstance());
        if (pose == null) {
            return;
        }

        setPosition(pose.x(), pose.y(), pose.z());
        setRotation(pose.yaw(), pose.pitch());
        detached = true;
    }

    @Inject(method = "calculateFov(F)F", at = @At("RETURN"), cancellable = true)
    private void freecamera$removePlayerDynamicFov(
            float partialTick,
            CallbackInfoReturnable<Float> callback
    ) {
        callback.setReturnValue(DetachedCameraFov.select(
                Minecraft.getInstance(),
                callback.getReturnValueF()
        ));
    }
}
