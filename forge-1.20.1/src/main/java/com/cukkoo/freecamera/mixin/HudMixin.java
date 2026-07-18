package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.state.CameraStateManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class HudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void freecamera$onRender(GuiGraphics graphics, float tickDelta, CallbackInfo ci) {
        if (CameraStateManager.getInstance().isActive()) {
            ci.cancel();
        }
    }
}
