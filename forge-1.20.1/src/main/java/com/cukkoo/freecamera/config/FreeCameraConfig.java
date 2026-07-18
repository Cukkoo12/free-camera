package com.cukkoo.freecamera.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

public final class FreeCameraConfig {

    private static final Path CONFIG_PATH = FMLPaths.CONFIGDIR.get().resolve("freecamera.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static FreeCameraConfig instance;

    public float flightSpeedMultiplier    = 1.0f;
    public float mouseSensitivity         = 1.0f;
    public float cinematicSmoothingFactor = 0.12f;
    public float orbitSensitivity         = 0.3f;
    public float zoomFactor               = 0.25f;
    public float rollIncrement            = 5.0f;
    public float scrollSpeedFactor        = 1.3f;
    public boolean invertOrbitY           = false;
    public boolean showSpeedInActionBar   = true;

    public FreeCameraConfig() { }

    public static FreeCameraConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public static FreeCameraConfig reload() {
        instance = load();
        return instance;
    }

    private static FreeCameraConfig load() {
        if (!Files.exists(CONFIG_PATH)) return new FreeCameraConfig();
        try {
            String json = Files.readString(CONFIG_PATH);
            FreeCameraConfig cfg = GSON.fromJson(json, FreeCameraConfig.class);
            if (cfg != null) {
                cfg.sanitise();
                return cfg;
            }
        } catch (Exception ignored) { }
        return new FreeCameraConfig();
    }

    public void save() {
        sanitise();
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (Exception ignored) { }
    }

    private void sanitise() {
        flightSpeedMultiplier    = clamp(flightSpeedMultiplier,    0.1f,  10.0f);
        mouseSensitivity         = clamp(mouseSensitivity,         0.1f,   5.0f);
        cinematicSmoothingFactor = clamp(cinematicSmoothingFactor, 0.01f,  0.5f);
        orbitSensitivity         = clamp(orbitSensitivity,         0.05f,  5.0f);
        zoomFactor               = clamp(zoomFactor,               0.05f,  1.0f);
        rollIncrement            = clamp(rollIncrement,            0.5f,  45.0f);
        scrollSpeedFactor        = clamp(scrollSpeedFactor,        1.05f,  3.0f);
    }

    private static float clamp(float v, float min, float max) {
        return v < min ? min : Math.min(v, max);
    }
}
