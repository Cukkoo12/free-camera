package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class HandMixin {

    @Inject(method = "renderItemInHand", at = @At("HEAD"), cancellable = true)
    private void freecamera$onRenderItemInHand(CallbackInfo ci) {
        if (CameraStateManager.getInstance().isActive()) {
            ci.cancel();
        }
    }
}
