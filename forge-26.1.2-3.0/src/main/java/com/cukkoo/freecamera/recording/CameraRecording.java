package com.cukkoo.freecamera.recording;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CameraRecording {
    public static final int SCHEMA_VERSION = 1;
    private String name = "Recording";
    private final ArrayList<CameraRecordingFrame> frames = new ArrayList<>();
    public String name() { return name; }
    public void setName(String name) { this.name = name; }
    public List<CameraRecordingFrame> frames() { return Collections.unmodifiableList(frames); }
    boolean add(CameraRecordingFrame frame, int limit) { if (frames.size() >= limit) return false; frames.add(frame); return true; }
    public int size() { return frames.size(); }
    public CameraRecordingFrame get(int index) { return frames.get(index); }
}
