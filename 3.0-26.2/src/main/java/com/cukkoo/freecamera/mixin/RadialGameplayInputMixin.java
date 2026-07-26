package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class RadialGameplayInputMixin {
    @Inject(method = "handleKeybinds", at = @At("HEAD"), cancellable = true)
    private void freecamera$suppressGameplayKeybinds(CallbackInfo ci) {
        if (FreeCameraClient.isRadialInputSuppressed()) {
            ci.cancel();
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void freecamera$suppressHeldAttack(boolean leftClick, CallbackInfo ci) {
        if (FreeCameraClient.isRadialInputSuppressed()) {
            ci.cancel();
        }
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void freecamera$suppressAttack(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCameraClient.isRadialInputSuppressed()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void freecamera$suppressUseItem(CallbackInfo ci) {
        if (FreeCameraClient.isRadialInputSuppressed()) {
            ci.cancel();
        }
    }
}
