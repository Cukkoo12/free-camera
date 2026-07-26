package com.cukkoo.freecamera.radial;

public final class CameraRadialMenuController {
    private final CameraRadialMenuState state;

    public CameraRadialMenuController(CameraRadialMenuState state) {
        this.state = state;
    }

    public CameraRadialMenuState state() {
        return state;
    }

    public void open() {
        state.open();
    }

    public void accumulateMouse(double deltaX, double deltaY, double guiScale) {
        double safeScale = Double.isFinite(guiScale) && guiScale > 0.0 ? guiScale : 1.0;
        state.accumulate(deltaX / safeScale, deltaY / safeScale);
    }

    public CameraRadialEntry confirmAndClose() {
        CameraRadialEntry selection = state.highlightedEntry();
        state.clear();
        return selection;
    }

    public void cancelAndClose() {
        state.clear();
    }

    public void clear() {
        state.clear();
    }
}
