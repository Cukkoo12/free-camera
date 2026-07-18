package com.cukkoo.freecamera.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON-based configuration for the Free Camera mod.
 * Singleton. Call {@link #getInstance()} to access.
 *
 * <p>Fields are public for direct read access by other classes.
 * After modifying fields call {@link #save()} to persist.</p>
 */
public final class FreeCameraConfig {

    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir().resolve("freecamera.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static FreeCameraConfig instance;

    // ── Settings ──────────────────────────────────────────────────

    /** Multiplier applied to the base flight speed. */
    public float flightSpeedMultiplier    = 1.0f;
    /** Mouse look sensitivity while in freecam mode (1.0 = default). */
    public float mouseSensitivity         = 1.0f;
    /** How fast the cinematic lerp converges (lower = smoother). */
    public float cinematicSmoothingFactor = 0.12f;
    /** Orbit mouse sensitivity. */
    public float orbitSensitivity         = 0.3f;
    /** FOV multiplier when zooming (V key). */
    public float zoomFactor               = 0.25f;
    /** Degrees added per roll key press. */
    public float rollIncrement            = 5.0f;
    /** Speed multiplier per scroll notch in freecam. */
    public float scrollSpeedFactor        = 1.3f;
    /** Invert the vertical orbit mouse axis. */
    public boolean invertOrbitY           = false;
    /** Show the current flight speed in the action bar. */
    public boolean showSpeedInActionBar   = true;

    public FreeCameraConfig() { }

    // ── Load / save ───────────────────────────────────────────────

    public static FreeCameraConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    /** Reload from disk. Discards any in-memory changes. */
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
                // Clamp all numeric fields to valid ranges
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
