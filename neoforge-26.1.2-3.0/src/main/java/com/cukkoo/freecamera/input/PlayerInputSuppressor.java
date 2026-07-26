package com.cukkoo.freecamera.input;

import com.cukkoo.freecamera.state.CameraStateMachine;
import net.minecraft.client.Minecraft;
import java.util.function.BooleanSupplier;

public final class PlayerInputSuppressor {
    public interface MutablePlayerMovementInput {
        void freecamera$clearMovementInput();
    }

    private final CameraStateMachine stateMachine;
    private final BooleanSupplier radialInputSuppressed;
    private boolean suppressedThisTick;

    public PlayerInputSuppressor(CameraStateMachine stateMachine) {
        this(stateMachine, () -> false);
    }

    public PlayerInputSuppressor(
            CameraStateMachine stateMachine,
            BooleanSupplier radialInputSuppressed
    ) {
        this.stateMachine = stateMachine;
        this.radialInputSuppressed = radialInputSuppressed;
    }

    public void suppressIfNeeded(Minecraft client, MutablePlayerMovementInput input) {
        suppressedThisTick = false;
        if ((!radialInputSuppressed.getAsBoolean()
                && !stateMachine.shouldSuppressPlayerMovementInput()) || client.screen != null) {
            return;
        }
        input.freecamera$clearMovementInput();
        suppressedThisTick = true;
    }

    public void clearTransientState() {
        suppressedThisTick = false;
    }

    boolean suppressedThisTickForDevelopmentCheck() {
        return suppressedThisTick;
    }
}
