package com.cukkoo.freecamera;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public class FreeCamera implements ClientModInitializer {

    public static FreeCameraConfig config;
    public static boolean enabled = true;
    public static boolean detached = false;

    public static double detachX, detachY, detachZ;

    private static boolean toggleKeyWasDown = false;
    private static boolean detachKeyWasDown = false;

    @Override
    public void onInitializeClient() {
        config = FreeCameraConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Window window = client.getWindow();
            if (window == null) return;

            boolean toggleKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_F6);
            if (toggleKeyDown && !toggleKeyWasDown) {
                enabled = !enabled;
                if (!enabled) resetCamera();
            }
            toggleKeyWasDown = toggleKeyDown;

            boolean detachKeyDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_F7);
            if (detachKeyDown && !detachKeyWasDown) {
                detached = !detached;
                if (detached && client.gameRenderer.getMainCamera() != null) {
                    detachX = client.gameRenderer.getMainCamera().position().x;
                    detachY = client.gameRenderer.getMainCamera().position().y;
                    detachZ = client.gameRenderer.getMainCamera().position().z;
                }
            }
            detachKeyWasDown = detachKeyDown;

            if (isActive()) {
                boolean zPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_Z);
                boolean cPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_C);
                boolean rPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_R);

                boolean changed = false;
                if (zPressed) {
                    config.cameraRoll -= 2.0;
                    changed = true;
                }
                if (cPressed) {
                    config.cameraRoll += 2.0;
                    changed = true;
                }
                if (rPressed) {
                    config.cameraRoll = 0.0;
                    config.cameraDistance = 4.0;
                    if (client.player != null) {
                        config.cameraYaw = client.player.getYRot();
                        config.cameraPitch = client.player.getXRot();
                    }
                    changed = true;
                }
                if (changed) {
                    config.save();
                }
            }
        });
    }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || !client.options.getCameraType().isMirrored()) {
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
        detached = false;
    }
}
