package com.cukkoo.freecamera.zoom;

import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

public final class CameraZoomController {
    private static final double MAX_SUBSTEP_SECONDS = 0.05;
    private static final double MAX_ACCEPTED_ELAPSED_SECONDS = 0.25;
    private static final int MAX_SUBSTEP_COUNT = 5;

    private final CameraZoomState state = new CameraZoomState();
    private final CameraZoomSmoother smoother = new CameraZoomSmoother();
    private KeyMapping holdZoom;
    private long lastFrameNanos;
    private boolean inputArmed;
    private double responsiveFrequency=12,cinematicFrequency=5;
    public void configure(double multiplier,double responsiveFrequency,double cinematicFrequency){state.configure(multiplier);this.responsiveFrequency=Math.clamp(responsiveFrequency,.1,40);this.cinematicFrequency=Math.clamp(cinematicFrequency,.1,40);}

    public void register(KeyMapping.Category category) {
        holdZoom = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.hold_zoom", GLFW.GLFW_KEY_V, category));
    }

    public void advance(Minecraft client, CinematicMotionProfile profile) {
        if (client.screen != null || !client.isWindowActive()) {
            suspendInput();
            return;
        }
        boolean zoomDown = holdZoom != null && holdZoom.isDown();
        if (!inputArmed) {
            if (!zoomDown) {
                inputArmed = true;
            }
            lastFrameNanos = 0L;
            return;
        }
        state.setTargetMultiplier(zoomDown
                ? state.configuredMultiplier()
                : CameraZoomState.NORMAL_MULTIPLIER);
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        double elapsed = (now - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = now;
        advanceAccepted(profile, elapsed);
    }

    public void advanceForDevelopmentCheck(CinematicMotionProfile profile, double elapsedSeconds) {
        advanceAccepted(profile, elapsedSeconds);
    }

    public void setHeldForDevelopmentCheck(boolean held) {
        state.setTargetMultiplier(held
                ? CameraZoomState.DEFAULT_ZOOM_MULTIPLIER
                : CameraZoomState.NORMAL_MULTIPLIER);
    }

    public CameraZoomState state() { return state; }

    public void suspendInput() {
        state.freezeAtCurrent();
        lastFrameNanos = 0L;
        inputArmed = false;
    }

    public void clear() {
        state.clear();
        lastFrameNanos = 0L;
        inputArmed = false;
    }

    private void advanceAccepted(CinematicMotionProfile profile, double elapsed) {
        if (!Double.isFinite(elapsed) || elapsed <= 0.0 || elapsed > MAX_ACCEPTED_ELAPSED_SECONDS) {
            lastFrameNanos = 0L;
            return;
        }
        int count = Math.min(MAX_SUBSTEP_COUNT,
                Math.max(1, (int) Math.ceil(elapsed / MAX_SUBSTEP_SECONDS)));
        double remaining = elapsed;
        for (int step = 0; step < count; step++) {
            double substep = Math.min(MAX_SUBSTEP_SECONDS, remaining);
            smoother.integrate(state, profile==CinematicMotionProfile.CINEMATIC?cinematicFrequency:responsiveFrequency, substep);
            remaining -= substep;
        }
    }
}
