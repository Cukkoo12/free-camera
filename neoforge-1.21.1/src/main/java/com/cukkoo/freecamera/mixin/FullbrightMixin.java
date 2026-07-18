package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LightTexture.class)
public class FullbrightMixin {

    @Inject(method = "getBrightness", at = @At("HEAD"), cancellable = true)
    private static void freecamera$onGetBrightness(DimensionType dimensionType, int light, CallbackInfoReturnable<Float> cir) {
        if (CameraStateManager.getInstance().isActive()) {
            cir.setReturnValue(1.0f);
        }
    }
}
