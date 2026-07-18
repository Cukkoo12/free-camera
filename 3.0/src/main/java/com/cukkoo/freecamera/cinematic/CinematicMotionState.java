package com.cukkoo.freecamera.cinematic;

import com.cukkoo.freecamera.api.CameraMode;

public final class CinematicMotionState {
    private final CinematicRotationSmoother rotationSmoother = new CinematicRotationSmoother();
    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public CinematicMotionProfile activeProfile(CameraMode mode) {
        return enabled && (mode == CameraMode.FREE_CAMERA || mode == CameraMode.ORBIT || mode == CameraMode.FOLLOW)
                ? CinematicMotionProfile.CINEMATIC
                : CinematicMotionProfile.RESPONSIVE;
    }

    public boolean toggle(CameraMode mode, float yaw, float pitch) {
        if (mode != CameraMode.FREE_CAMERA && mode != CameraMode.ORBIT && mode != CameraMode.FOLLOW) {
            return false;
        }
        enabled = !enabled;
        if (enabled) {
            rotationSmoother.initialize(yaw, pitch);
        } else {
            rotationSmoother.clear();
        }
        return true;
    }

    public CinematicRotationSmoother rotationSmoother() {
        return rotationSmoother;
    }

    public void prepareMode(CameraMode mode, float yaw, float pitch) {
        if (enabled && (mode == CameraMode.FREE_CAMERA || mode == CameraMode.ORBIT || mode == CameraMode.FOLLOW)) {
            rotationSmoother.initialize(yaw, pitch);
        } else {
            rotationSmoother.clear();
        }
    }

    public void suspend(float yaw, float pitch) {
        if (enabled) {
            rotationSmoother.suspendAt(yaw, pitch);
        }
    }

    public void clear() {
        rotationSmoother.clear();
    }
    public void setEnabledPreference(boolean value){enabled=value;if(!value)rotationSmoother.clear();}
}
