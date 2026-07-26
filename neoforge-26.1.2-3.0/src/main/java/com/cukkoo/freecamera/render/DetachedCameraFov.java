package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.FreeCameraClient;
import net.minecraft.client.Minecraft;

/** Selects the world FOV without mutating Minecraft's global FOV option. */
public final class DetachedCameraFov {
    private DetachedCameraFov() {
    }

    public static float select(Minecraft client, float vanillaFov) {
        Integer configured = client.options.fov().get();
        return select(
                FreeCameraClient.stateMachineOrNull() != null
                        && FreeCameraClient.stateMachineOrNull().isActive(),
                configured == null ? vanillaFov : configured.floatValue(),
                vanillaFov
        );
    }

    public static float select(boolean active, float configuredBaseFov, float vanillaFov) {
        return active && Float.isFinite(configuredBaseFov) && configuredBaseFov > 0.0F
                ? configuredBaseFov : vanillaFov;
    }
}
