package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreecamController;
import com.cukkoo.freecamera.OrbitController;
import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.cukkoo.freecamera.state.CameraMode;
import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Vec3 position;
    @Shadow private Quaternionf rotation;
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    @Unique private boolean wasActive;
    @Unique private boolean applyingRotation;
    @Unique private double smX, smY, smZ;
    @Unique private float smYaw, smPitch, smRoll;

    @Inject(method = "setRotation(FF)V", at = @At("RETURN"))
    private void afterSetRotation(float yRot, float xRot, CallbackInfo ci) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) { wasActive = false; return; }
        if (applyingRotation) return;

        if (!wasActive) {
            wasActive = true;
            smX = smY = smZ = Double.NaN;
            smYaw = smPitch = smRoll = Float.NaN;
        }

        applyingRotation = true;

        float targetYaw, targetPitch;
        if (state.isLocked()) {
            targetYaw   = state.getLockedYaw();
            targetPitch = state.getLockedPitch();
        } else {
            Minecraft client = Minecraft.getInstance();
            LocalPlayer player = client.player;

            switch (state.getMode()) {
                case FREECAM -> {
                    targetYaw   = state.getCameraYaw();
                    targetPitch = state.getCameraPitch();
                }
                case ORBIT -> {
                    float playerYaw = player != null ? player.getYRot() : 0;
                    targetYaw   = playerYaw + OrbitController.getTheta();
                    targetPitch = OrbitController.getPhi();
                }
                default -> { applyingRotation = false; return; }
            }
        }
        targetPitch = Math.max(-90.0f, Math.min(90.0f, targetPitch));

        if (state.isCinematicMode() && !Float.isNaN(smYaw)) {
            float factor = state.getSmoothingFactor();
            float diff   = targetYaw - smYaw;
            if (diff > 180.0f)  diff -= 360.0f;
            if (diff < -180.0f) diff += 360.0f;
            smYaw   = smYaw + diff * factor;
            smPitch = smPitch + (targetPitch - smPitch) * factor;
            this.setRotation(smYaw, smPitch);
        } else {
            smYaw   = targetYaw;
            smPitch = targetPitch;
            this.setRotation(targetYaw, targetPitch);
        }

        applyingRotation = false;
    }

    @Inject(method = "setup", at = @At("RETURN"))
    private void afterSetup(CallbackInfo ci) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) return;

        Minecraft client = Minecraft.getInstance();

        if (!state.isLocked() && state.getMode() == CameraMode.FREECAM) {
            tickFreecam(client, 1.0f, state);
        }

        if (!state.isLocked() && state.getMode() == CameraMode.ORBIT) {
            LocalPlayer player = client.player;
            if (player != null) {
                Vec3 orbitPos = OrbitController.computeCameraPosition(player);
                state.syncCurrentPosition(orbitPos.x, orbitPos.y, orbitPos.z);
                state.syncCurrentYaw(player.getYRot() + OrbitController.getTheta());
                state.syncCurrentPitch(OrbitController.getPhi());
            }
        }

        double tx, ty, tz;
        if (state.isLocked()) {
            tx = state.getLockedX();
            ty = state.getLockedY();
            tz = state.getLockedZ();
        } else if (state.getMode() == CameraMode.FREECAM) {
            tx = state.getCameraX();
            ty = state.getCameraY();
            tz = state.getCameraZ();
        } else {
            this.detached = true;
            return;
        }

        if (state.isCinematicMode() && !Double.isNaN(smX)) {
            double factor = state.getSmoothingFactor();
            smX += (tx - smX) * factor;
            smY += (ty - smY) * factor;
            smZ += (tz - smZ) * factor;
            this.setPosition(smX, smY, smZ);
        } else {
            smX = tx; smY = ty; smZ = tz;
            this.setPosition(tx, ty, tz);
        }

        this.detached = true;
    }

    @Inject(method = "getMaxZoom(D)D", at = @At("RETURN"), cancellable = true)
    private void onGetMaxZoom(double startDistance, CallbackInfoReturnable<Double> cir) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (state.isActive() && state.getMode() == CameraMode.ORBIT) {
            cir.setReturnValue((double) OrbitController.getRadius());
        }
    }


    private static void tickFreecam(Minecraft client, float tickDelta, CameraStateManager state) {
        boolean forward = client.options.keyUp.isDown();
        boolean back    = client.options.keyDown.isDown();
        boolean left    = client.options.keyLeft.isDown();
        boolean right   = client.options.keyRight.isDown();
        boolean up      = client.options.keyJump.isDown();
        boolean down    = client.options.keyShift.isDown();

        float fwd  = (forward ? 1 : 0) - (back  ? 1 : 0);
        float str  = (right  ? 1 : 0) - (left  ? 1 : 0);
        float upDn = (up     ? 1 : 0) - (down  ? 1 : 0);

        float roll = state.getCameraRoll();
        if (Math.abs(roll) > 0.001f) {
            double rollRad = Math.toRadians(roll);
            double cosR = Math.cos(rollRad);
            double sinR = Math.sin(rollRad);
            double ns = str * cosR - upDn * sinR;
            double nu = str * sinR + upDn * cosR;
            str  = (float) ns;
            upDn = (float) nu;
        }

        float pitch = state.getCameraPitch();
        float yaw   = state.getCameraYaw();

        Vec3 forwardVec = Vec3.directionFromRotation(pitch, yaw);
        Vec3 rightVec   = Vec3.directionFromRotation(0, yaw + 90);

        double dx = forwardVec.x * fwd + rightVec.x * str;
        double dy = forwardVec.y * fwd + upDn;
        double dz = forwardVec.z * fwd + rightVec.z * str;

        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len > 1e-4) {
            dx /= len;
            dy /= len;
            dz /= len;
        }

        float speed = FreecamController.getCurrentSpeed()
                    * FreeCameraConfig.getInstance().flightSpeedMultiplier;

        state.setCameraPosition(
            state.getCameraX() + dx * speed * tickDelta,
            state.getCameraY() + dy * speed * tickDelta,
            state.getCameraZ() + dz * speed * tickDelta
        );
    }
}
