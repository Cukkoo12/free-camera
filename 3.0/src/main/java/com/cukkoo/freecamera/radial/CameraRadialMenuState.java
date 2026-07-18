package com.cukkoo.freecamera.radial;

public final class CameraRadialMenuState {
    public static final double DEAD_ZONE_RADIUS = 24.0;
    public static final double MAXIMUM_SELECTION_RADIUS = 74.0;

    private boolean open;
    private double selectionX;
    private double selectionY;
    private int highlightedIndex = -1;
    private double deadZoneRadius = DEAD_ZONE_RADIUS;

    public boolean isOpen() { return open; }
    public double selectionX() { return selectionX; }
    public double selectionY() { return selectionY; }
    public int highlightedIndex() { return highlightedIndex; }
    public CameraRadialEntry highlightedEntry() { return CameraRadialEntry.at(highlightedIndex); }

    void open() {
        open = true;
        selectionX = 0.0;
        selectionY = 0.0;
        highlightedIndex = -1;
    }

    void accumulate(double deltaX, double deltaY) {
        if (!open || !Double.isFinite(deltaX) || !Double.isFinite(deltaY)) {
            return;
        }
        selectionX += deltaX;
        selectionY += deltaY;
        double lengthSquared = selectionX * selectionX + selectionY * selectionY;
        double maximumSquared = MAXIMUM_SELECTION_RADIUS * MAXIMUM_SELECTION_RADIUS;
        if (lengthSquared > maximumSquared) {
            double scale = MAXIMUM_SELECTION_RADIUS / Math.sqrt(lengthSquared);
            selectionX *= scale;
            selectionY *= scale;
            lengthSquared = maximumSquared;
        }
        highlightedIndex = lengthSquared < deadZoneRadius * deadZoneRadius
                ? -1
                : CameraRadialEntry.indexForVector(selectionX, selectionY);
    }

    void clear() {
        open = false;
        selectionX = 0.0;
        selectionY = 0.0;
        highlightedIndex = -1;
    }
    public void setDeadZoneRadius(double radius) { deadZoneRadius=Double.isFinite(radius)?Math.clamp(radius,4,80):DEAD_ZONE_RADIUS; }
}
