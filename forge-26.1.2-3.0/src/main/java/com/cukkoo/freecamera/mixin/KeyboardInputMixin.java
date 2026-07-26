package com.cukkoo.freecamera.mixin;

import com.cukkoo.freecamera.FreeCameraClient;
import com.cukkoo.freecamera.input.PlayerInputSuppressor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class KeyboardInputMixin implements PlayerInputSuppressor.MutablePlayerMovementInput {
    @Inject(method = "tick()V", at = @At("RETURN"))
    private void freecamera$neutralizePlayerMovementInput(CallbackInfo ci) {
        PlayerInputSuppressor suppressor = FreeCameraClient.playerInputSuppressorOrNull();
        if (suppressor != null) {
            suppressor.suppressIfNeeded(Minecraft.getInstance(), this);
        }
    }

    @Override
    public void freecamera$clearMovementInput() {
        ClientInput input = (ClientInput) (Object) this;
        input.keyPresses = Input.EMPTY;
        ((ClientInputAccessor) input).freecamera$setMoveVector(Vec2.ZERO);
    }
}
