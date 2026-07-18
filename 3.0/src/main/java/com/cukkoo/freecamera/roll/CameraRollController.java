package com.cukkoo.freecamera.roll;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.cinematic.CinematicMotionProfile;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public final class CameraRollController {
    private static final double MAX_SUBSTEP_SECONDS = 0.05;
    private static final double MAX_ACCEPTED_ELAPSED_SECONDS = 0.25;
    private static final int MAX_SUBSTEP_COUNT = 5;

    private final CameraRollState state = new CameraRollState();
    private final CameraRollSmoother smoother = new CameraRollSmoother();
    private KeyMapping rollLeft;
    private KeyMapping rollRight;
    private long lastFrameNanos;
    private boolean inputArmed;
    private double rate=CameraRollState.DEFAULT_RATE,responsiveFrequency=3.624,cinematicFrequency=2.2;
    public void configure(double rate,double responsiveFrequency,double cinematicFrequency){this.rate=Math.clamp(rate,1,360);this.responsiveFrequency=Math.clamp(responsiveFrequency,.1,40);this.cinematicFrequency=Math.clamp(cinematicFrequency,.1,40);}

    public void register(KeyMapping.Category category, int defaultLeftKey, int defaultRightKey) {
        rollLeft = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.roll_left", defaultLeftKey, category));
        rollRight = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.free-camera.roll_right", defaultRightKey, category));
    }

    public void advance(Minecraft client, CameraMode mode, CinematicMotionProfile profile) {
        if (mode == CameraMode.TRIPOD || client.screen != null || !client.isWindowActive()) {
            suspendInput();
            return;
        }
        boolean leftDown = rollLeft != null && rollLeft.isDown();
        boolean rightDown = rollRight != null && rollRight.isDown();
        if (!inputArmed) {
            if (!leftDown && !rightDown) {
                inputArmed = true;
            }
            lastFrameNanos = 0L;
            return;
        }
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return;
        }
        double elapsed = (now - lastFrameNanos) * 1.0E-9;
        lastFrameNanos = now;
        int direction = leftDown == rightDown ? 0 : (leftDown ? -1 : 1);
        if (direction == 0) {
            state.holdCurrent();
            return;
        }
        advanceAccepted(profile, elapsed, direction);
    }

    public void advanceForDevelopmentCheck(
            CinematicMotionProfile profile,
            double elapsedSeconds,
            int direction
    ) {
        if (direction == 0) {
            state.holdCurrent();
            return;
        }
        advanceAccepted(profile, elapsedSeconds, Integer.signum(direction));
    }

    public CameraRollState state() {
        return state;
    }

    public void resetToZero() {
        state.resetToZero();
        lastFrameNanos = 0L;
        inputArmed = false;
    }

    public boolean resetToZeroIfNeeded() {
        if (!CameraResetPolicy.shouldResetRoll(
                state.currentRoll(), state.velocity()
        )) {
            return false;
        }
        resetToZero();
        return true;
    }

    public void suspendInput() {
        state.holdCurrent();
        lastFrameNanos = 0L;
        inputArmed = false;
    }

    public void clear() {
        state.clear();
        lastFrameNanos = 0L;
        inputArmed = false;
    }

    private void advanceAccepted(CinematicMotionProfile profile, double elapsed, int direction) {
        if (!Double.isFinite(elapsed) || elapsed <= 0.0 || elapsed > MAX_ACCEPTED_ELAPSED_SECONDS) {
            lastFrameNanos = 0L;
            state.holdCurrent();
            return;
        }
        int count = Math.min(MAX_SUBSTEP_COUNT,
                Math.max(1, (int) Math.ceil(elapsed / MAX_SUBSTEP_SECONDS)));
        double remaining = elapsed;
        for (int step = 0; step < count; step++) {
            double substep = Math.min(MAX_SUBSTEP_SECONDS, remaining);
            smoother.integrate(
                    state,
                    direction * rate,
                    profile==CinematicMotionProfile.CINEMATIC?cinematicFrequency:responsiveFrequency,
                    substep
            );
            remaining -= substep;
        }
    }
}
