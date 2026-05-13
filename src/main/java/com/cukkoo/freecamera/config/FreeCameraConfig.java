package com.cukkoo.freecamera.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class FreeCameraConfig {
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("free-camera.json");

    public double cameraDistance = 4.0;
    public double rotationSensitivity = 1.0;
    public double smoothness = 0.0;
    public double fovZoom = 1.0;
    public double cameraRoll = 0.0;

    public transient float cameraYaw;
    public transient float cameraPitch;
    public transient boolean initialized;

    public static FreeCameraConfig load() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                return new Gson().fromJson(Files.readString(CONFIG_PATH), FreeCameraConfig.class);
            }
        } catch (Exception e) {
            /* ignore corrupt config, use defaults */
        }
        return new FreeCameraConfig();
    }

    public void save() {
        try {
            Files.writeString(CONFIG_PATH,
                    new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception e) {
            /* ignore save failures */
        }
    }
}
