package com.cukkoo.freecamera.render;

import com.cukkoo.freecamera.roll.CameraOrientationBridge;
import com.cukkoo.freecamera.zoom.CameraProjectionBridge;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public final class CanonicalWorldView {
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private boolean ready;

    public void prepare(
            Matrix4fc vanillaView,
            Matrix4fc vanillaProjection,
            float yaw,
            float pitch,
            double rollDegrees,
            double zoomMultiplier,
            CameraOrientationBridge orientationBridge,
            CameraProjectionBridge projectionBridge
    ) {
        view.set(vanillaView);
        projection.set(vanillaProjection);
        orientationBridge.applyWorldView(view, yaw, pitch, rollDegrees);
        projectionBridge.apply(projection, zoomMultiplier);
        ready = true;
    }

    public Matrix4fc selectViewOrVanilla(Matrix4fc vanillaView) {
        return ready ? view : vanillaView;
    }

    public Matrix4fc view() {
        return view;
    }

    public Matrix4fc projection() {
        return projection;
    }

    Matrix4f mutableView() {
        return view;
    }

    Matrix4f mutableProjection() {
        return projection;
    }

    public boolean isReady() {
        return ready;
    }

    public void clear() {
        ready = false;
    }
}
