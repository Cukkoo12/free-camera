package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardRadialMenuMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void freecamera$cancelRadialWithEscape(
            long window,
            int action,
            KeyEvent event,
            CallbackInfo ci
    ) {
        if (FreeCameraClient.isRadialMenuOpen()
                && action == GLFW.GLFW_PRESS
                && event.key() == GLFW.GLFW_KEY_ESCAPE) {
            FreeCameraClient.cancelRadialMenu();
            ci.cancel();
        }
    }
}
