package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ClientLifecycleBridgeMixin {
    @Inject(method = "tick", at = @At("RETURN"))
    private void freecamera$endClientTick(CallbackInfo ci) {
        FreeCameraClient.endClientTick((Minecraft) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void freecamera$clientStopping(CallbackInfo ci) {
        FreeCameraClient.shutdown((Minecraft) (Object) this);
    }
}
