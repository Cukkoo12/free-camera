package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraMode;

public record CameraRecordingFrame(double time, double x, double y, double z, float yaw, float pitch,
                                   double roll, double zoom, CameraMode mode) { }
