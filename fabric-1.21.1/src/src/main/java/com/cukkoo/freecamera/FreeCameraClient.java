package com.cukkoo.freecamera;

import com.cukkoo.freecamera.config.FreeCameraConfig;
import com.cukkoo.freecamera.config.FreeCameraSettingsScreen;
import com.cukkoo.freecamera.state.CameraStateManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class FreeCameraClient implements ClientModInitializer {

    public static final List<KeyMapping> KEY_MAPPINGS = new ArrayList<>();
    private static boolean mappingsRegistered;

    private CameraType previousCameraType;

    public static final KeyMapping TOGGLE_KEY    = register("key.freecamera.toggle",    GLFW.GLFW_KEY_F6);
    public static final KeyMapping RESET_KEY     = register("key.freecamera.reset",      GLFW.GLFW_KEY_R);
    public static final KeyMapping TRIPOD_KEY    = register("key.freecamera.tripod",     GLFW.GLFW_KEY_F7);
    public static final KeyMapping CINEMATIC_KEY = register("key.freecamera.cinematic",  GLFW.GLFW_KEY_F8);
    public static final KeyMapping SETTINGS_KEY  = register("key.freecamera.settings",   GLFW.GLFW_KEY_F9);
    public static final KeyMapping ROLL_LEFT_KEY = register("key.freecamera.roll_left",  GLFW.GLFW_KEY_Z);
    public static final KeyMapping ROLL_RIGHT_KEY= register("key.freecamera.roll_right", GLFW.GLFW_KEY_C);

    private static KeyMapping register(String name, int key) {
        KeyMapping km = new KeyMapping(name, key, "freecamera.category");
        KEY_MAPPINGS.add(km);
        return km;
    }

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndTick);
    }

    private void onEndTick(Minecraft client) {
        if (!mappingsRegistered) {
            try {
                java.lang.reflect.Field f = client.options.getClass().getDeclaredField("keyMappings");
                f.setAccessible(true);
                KeyMapping[] existing = (KeyMapping[]) f.get(client.options);
                KeyMapping[] combined = java.util.Arrays.copyOf(existing, existing.length + KEY_MAPPINGS.size());
                for (int i = 0; i < KEY_MAPPINGS.size(); i++) {
                    combined[existing.length + i] = KEY_MAPPINGS.get(i);
                }
                f.set(client.options, combined);
            } catch (Exception ignored) { }
            mappingsRegistered = true;
        }

        if (client.player == null) return;

        while (TOGGLE_KEY.consumeClick())    FreecamController.toggle(client);
        while (RESET_KEY.consumeClick())     FreecamController.reset(client);
        while (TRIPOD_KEY.consumeClick())    FreecamController.toggleTripod(client);
        while (CINEMATIC_KEY.consumeClick()) FreecamController.toggleCinematic(client);

        while (SETTINGS_KEY.consumeClick()) {
            client.setScreen(new FreeCameraSettingsScreen(client.screen));
        }

        float rollInc = FreeCameraConfig.getInstance().rollIncrement;
        while (ROLL_LEFT_KEY.consumeClick()) FreecamController.roll(-rollInc);
        while (ROLL_RIGHT_KEY.consumeClick())FreecamController.roll(rollInc);

        CameraType currentType = client.options.getCameraType();
        if (previousCameraType != null && previousCameraType != currentType) {
            CameraStateManager state = CameraStateManager.getInstance();
            if (state.isActive()) {
                FreecamController.deactivate(client);
            }
        }
        previousCameraType = currentType;

        FreecamController.tick(client);
    }
}
