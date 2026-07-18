package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreecamController;
import com.cukkoo.freecamera.OrbitController;
import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseMixin {

    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    /**
     * Capture mouse deltas at turnPlayer HEAD, apply sensitivity and
     * roll-rotation, write them to CameraStateManager, then cancel so
     * vanilla does not turn the player entity.
     */
    @Inject(method = "turnPlayer", at = @At("HEAD"), cancellable = true)
    private void freecamera$onTurnPlayer(double mousea, CallbackInfo ci) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) return;
        if (state.isLocked()) return;

        Minecraft client = Minecraft.getInstance();

        // ── F5 / perspective-change safety check ─────────────────
        // Detect vanilla camera-type changes immediately (in the render
        // loop, before the END_CLIENT_TICK handler fires) so the player
        // never feels locked after pressing F5.
        if (state.getActivationCameraType() != null
                && client.options.getCameraType() != state.getActivationCameraType()) {
            FreecamController.deactivate(client);
            return; // let vanilla handle this frame
        }

        // Vanilla sensitivity formula: ss = sens * 0.6 + 0.2, sens = ss^3 * 8.0
        double ss = client.options.sensitivity().get() * 0.6 + 0.2;
        double sens = ss * ss * ss * 8.0;

        double dx = this.accumulatedDX * sens;
        double dy = this.accumulatedDY * sens;

        // Config multiplier
        float sensMul = FreeCameraConfig.getInstance().mouseSensitivity;
        dx *= sensMul;
        dy *= sensMul;

        // ── Roll-rotated mouse axes ──────────────────────────────
        // Rotate the mouse delta by the camera's current roll so that
        // screen-space mouse movement maps correctly regardless of how
        // much the camera is tilted (Z / C keys).
        float roll = state.getCameraRoll();
        if (Math.abs(roll) > 0.001f) {
            double rollRad = Math.toRadians(roll);
            double cosR = Math.cos(rollRad);
            double sinR = Math.sin(rollRad);
            double rdX = dx * cosR - dy * sinR;
            double rdY = dx * sinR + dy * cosR;
            dx = rdX;
            dy = rdY;
        }

        // Invert settings (applied after roll rotation so the inverted
        // axes are also screen-aligned).
        if (client.options.invertYMouse().get()) dy = -dy;

        switch (state.getMode()) {
            case FREECAM -> {
                state.setCameraYaw(state.getCameraYaw() + (float) dx);
                state.setCameraPitch(
                    Math.clamp(state.getCameraPitch() + (float) dy, -90.0f, 90.0f)
                );
            }
            case ORBIT -> {
                OrbitController.onMouseMove(dx, dy);
            }
        }

        this.accumulatedDX = 0;
        this.accumulatedDY = 0;
        ci.cancel();
    }

    /**
     * Capture scroll-wheel input for flight-speed (freecam) or
     * orbit-radius (orbit) control. Cancels the scroll so hotbar / FOV
     * are unaffected while a camera mode is active.
     */
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void freecamera$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive() || state.isLocked()) return;

        FreecamController.onScroll(vertical);
        ci.cancel();
    }
}
