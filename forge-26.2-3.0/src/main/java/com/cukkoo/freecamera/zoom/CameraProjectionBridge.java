package com.cukkoo.freecamera.zoom;

import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.joml.Matrix4f;

public final class CameraProjectionBridge {
    public void apply(CameraRenderState cameraState, double zoomMultiplier) {
        apply(cameraState.projectionMatrix, zoomMultiplier);
    }

    public void apply(Matrix4f projectionMatrix, double zoomMultiplier) {
        if (!Double.isFinite(zoomMultiplier) || zoomMultiplier <= 1.0) {
            return;
        }
        float scale = (float) zoomMultiplier;
        projectionMatrix.m00(projectionMatrix.m00() * scale);
        projectionMatrix.m11(projectionMatrix.m11() * scale);
    }
}
