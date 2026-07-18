package com.cukkoo.freecamera.input;

import com.cukkoo.freecamera.state.CameraStateMachine;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import java.util.function.BooleanSupplier;

public final class CameraInputRouter {
    private final CameraStateMachine stateMachine;
    private final CameraInputSnapshot snapshot = new CameraInputSnapshot();
    private final BooleanSupplier radialInputSuppressed;

    public CameraInputRouter(CameraStateMachine stateMachine) {
        this(stateMachine, () -> false);
    }

    public CameraInputRouter(
            CameraStateMachine stateMachine,
            BooleanSupplier radialInputSuppressed
    ) {
        this.stateMachine = stateMachine;
        this.radialInputSuppressed = radialInputSuppressed;
    }

    public CameraInputSnapshot sample(Minecraft client) {
        snapshot.clear();
        if (radialInputSuppressed.getAsBoolean()
                || !stateMachine.shouldSuppressPlayerMovementInput()
                || client.screen != null
                || !client.isWindowActive()) {
            return snapshot;
        }

        Options options = client.options;
        double forward = axis(options.keyUp.isDown(), options.keyDown.isDown());
        double strafe = axis(options.keyRight.isDown(), options.keyLeft.isDown());
        double vertical = axis(options.keyJump.isDown(), options.keyShift.isDown());
        snapshot.set(forward, strafe, vertical);
        return snapshot;
    }

    public void clear() {
        snapshot.clear();
    }

    private static double axis(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0;
        }
        return positive ? 1.0 : -1.0;
    }
}
