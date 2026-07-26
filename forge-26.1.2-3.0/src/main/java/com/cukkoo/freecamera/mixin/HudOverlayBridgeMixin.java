package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudOverlayBridgeMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void freecamera$renderOverlays(
            GuiGraphicsExtractor graphics,
            DeltaTracker tracker,
            CallbackInfo ci
    ) {
        FreeCameraClient.renderHud(graphics, tracker);
    }
}
