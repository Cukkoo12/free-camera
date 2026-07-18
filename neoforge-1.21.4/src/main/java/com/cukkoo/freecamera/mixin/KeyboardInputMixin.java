package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreecamController;
import com.cukkoo.freecamera.state.CameraMode;
import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cancels vanilla player movement input while freecam or orbit is active.
 * Skips cancellation during locked (tripod) mode so the player can move
 * normally.  Also auto-deactivates on F5 / perspective change.
 */
@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin extends ClientInput {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void freecamera$onTick(CallbackInfo ci) {
        CameraStateManager state = CameraStateManager.getInstance();
        if (!state.isActive()) return;
        if (state.isLocked()) return;

        // Orbit — player walks/runs/jumps normally, camera follows
        if (state.getMode() == CameraMode.ORBIT) return;

        // ── F5 / perspective-change safety check ─────────────────
        Minecraft client = Minecraft.getInstance();
        if (state.getActivationCameraType() != null
                && client.options.getCameraType() != state.getActivationCameraType()) {
            FreecamController.deactivate(client);
            return; // let vanilla handle this frame
        }

        this.leftImpulse = 0;
        this.forwardImpulse = 0;
        ci.cancel();
    }
}
