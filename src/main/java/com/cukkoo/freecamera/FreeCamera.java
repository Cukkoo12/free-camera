package com.cukkoo.freecamera;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public class FreeCamera implements ClientModInitializer {

    public static FreeCameraConfig config;

    @Override
    public void onInitializeClient() {
        config = FreeCameraConfig.load();
    }

    public static boolean isActive() {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.getCameraType().isFirstPerson()) {
            return false;
        }

        if (!config.initialized) {
            config.cameraYaw = client.player.getYRot();
            config.cameraPitch = client.player.getXRot();
            config.initialized = true;
        }

        return true;
    }

    public static void resetCamera() {
        config.initialized = false;
    }
}
