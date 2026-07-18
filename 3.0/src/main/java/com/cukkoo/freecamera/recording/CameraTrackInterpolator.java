package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraPose;
import net.minecraft.util.Mth;

public final class CameraTrackInterpolator {
    public void apply(CameraRecordingFrame a, CameraRecordingFrame b, double alpha, CameraPose pose) {
        alpha = Math.clamp(alpha, 0.0, 1.0);
        pose.set(a.x()+(b.x()-a.x())*alpha, a.y()+(b.y()-a.y())*alpha, a.z()+(b.z()-a.z())*alpha,
                Mth.wrapDegrees(a.yaw()+Mth.wrapDegrees(b.yaw()-a.yaw())*(float)alpha),
                Mth.clamp(a.pitch()+(b.pitch()-a.pitch())*(float)alpha,-90,90));
    }
    public double roll(CameraRecordingFrame a, CameraRecordingFrame b, double alpha) { return a.roll()+(b.roll()-a.roll())*alpha; }
    public double zoom(CameraRecordingFrame a, CameraRecordingFrame b, double alpha) {
        double value=a.zoom()+(b.zoom()-a.zoom())*alpha; return Double.isFinite(value) ? Math.clamp(value,1,10) : 1;
    }
}
