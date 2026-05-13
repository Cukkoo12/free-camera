package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCamera;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    private float yRot;

    @Shadow
    private float xRot;

    @Shadow
    protected abstract void setPosition(double x, double y, double z);

    @Shadow public abstract net.minecraft.world.phys.Vec3 position();

    @Shadow
    protected abstract void setRotation(float yRot, float xRot);

    @Unique
    private boolean freecam$applyingOffset;

    @Unique
    private double smoothedX = Double.NaN, smoothedY = Double.NaN, smoothedZ = Double.NaN;

    @Unique
    private float smoothedYaw = Float.NaN, smoothedPitch = Float.NaN;

    @Unique
    private float smoothedRoll = Float.NaN;

    @Unique
    private boolean freecam$wasActive = false;

    @Inject(method = "setRotation(FF)V", at = @At("RETURN"))
    private void afterSetRotation(float yaw, float pitch, CallbackInfo ci) {
        boolean active = FreeCamera.isActive();
        if (!active) {
            freecam$wasActive = false;
            return;
        }
        if (freecam$applyingOffset) return;

        if (!freecam$wasActive) {
            smoothedX = Double.NaN;
            smoothedY = Double.NaN;
            smoothedZ = Double.NaN;
            smoothedYaw = Float.NaN;
            smoothedPitch = Float.NaN;
            smoothedRoll = Float.NaN;
            freecam$wasActive = true;
        }

        freecam$applyingOffset = true;

        float targetYaw = FreeCamera.config.cameraYaw;
        float targetPitch = Math.clamp(FreeCamera.config.cameraPitch, -90f, 90f);

        if (FreeCamera.config.smoothness > 0.0) {
            float factor = (float) Math.clamp(1.0 - FreeCamera.config.smoothness, 0.01, 1.0);
            if (Float.isNaN(smoothedYaw) || Math.abs(smoothedYaw - targetYaw) > 180) {
                smoothedYaw = targetYaw;
                smoothedPitch = targetPitch;
            }
            smoothedYaw += (targetYaw - smoothedYaw) * factor;
            smoothedPitch += (targetPitch - smoothedPitch) * factor;

            this.setRotation(smoothedYaw, smoothedPitch);
        } else {
            smoothedYaw = targetYaw;
            smoothedPitch = targetPitch;
            this.setRotation(targetYaw, targetPitch);
        }

        freecam$applyingOffset = false;
    }

    @Inject(method = "update(Lnet/minecraft/client/DeltaTracker;)V", at = @At("RETURN"))
    private void afterUpdate(DeltaTracker tracker, CallbackInfo ci) {
        if (!FreeCamera.isActive()) return;

        double targetX, targetY, targetZ;
        if (FreeCamera.detached) {
            targetX = FreeCamera.detachX;
            targetY = FreeCamera.detachY;
            targetZ = FreeCamera.detachZ;
        } else {
            targetX = this.position().x;
            targetY = this.position().y;
            targetZ = this.position().z;
        }

        if (FreeCamera.config.smoothness > 0.0) {
            double factor = Math.clamp(1.0 - FreeCamera.config.smoothness, 0.01, 1.0);
            if (Double.isNaN(smoothedX) || Math.abs(smoothedX - targetX) > 100) {
                smoothedX = targetX;
                smoothedY = targetY;
                smoothedZ = targetZ;
            }
            smoothedX += (targetX - smoothedX) * factor;
            smoothedY += (targetY - smoothedY) * factor;
            smoothedZ += (targetZ - smoothedZ) * factor;

            this.setPosition(smoothedX, smoothedY, smoothedZ);
        } else {
            smoothedX = targetX;
            smoothedY = targetY;
            smoothedZ = targetZ;
            if (FreeCamera.detached) {
                this.setPosition(targetX, targetY, targetZ);
            }
        }

        float targetRoll = (float) FreeCamera.config.cameraRoll;
        float rollFactor = (FreeCamera.config.smoothness > 0.0) 
            ? (float) Math.clamp(1.0 - FreeCamera.config.smoothness, 0.01, 1.0)
            : 0.3f; // Fallback lerp factor for roll to prevent 20Hz stutter when smoothness is 0

        if (Float.isNaN(smoothedRoll) || Math.abs(smoothedRoll - targetRoll) > 100) {
            smoothedRoll = targetRoll;
        }
        smoothedRoll += (targetRoll - smoothedRoll) * rollFactor;
    }

    @Inject(method = "getMaxZoom(F)F", at = @At("RETURN"), cancellable = true)
    private void onGetMaxZoom(float startDistance, CallbackInfoReturnable<Float> cir) {
        if (!FreeCamera.isActive()) return;
        cir.setReturnValue((float) FreeCamera.config.cameraDistance);
    }

    @Inject(method = "getFov()F", at = @At("RETURN"), cancellable = true)
    private void onGetFov(CallbackInfoReturnable<Float> cir) {
        if (FreeCamera.isActive() && FreeCamera.config.fovZoom != 1.0) {
            float baseFov = cir.getReturnValue();
            double ratio = FreeCamera.config.cameraDistance / 4.0;
            // fovZoom scales the zoom effect
            ratio = 1.0 + (ratio - 1.0) * FreeCamera.config.fovZoom;
            if (ratio < 0.1) ratio = 0.1;
            cir.setReturnValue((float) (baseFov * ratio));
        }
    }

    @Inject(method = "getViewRotationMatrix(Lorg/joml/Matrix4f;)Lorg/joml/Matrix4f;", at = @At("RETURN"))
    private void onGetViewRotationMatrix(Matrix4f out, CallbackInfoReturnable<Matrix4f> cir) {
        if (FreeCamera.isActive() && Math.abs(smoothedRoll) > 0.01f) {
            Matrix4f mat = cir.getReturnValue();
            mat.rotateZ((float) Math.toRadians(smoothedRoll));
        }
    }
}
