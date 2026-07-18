package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;

public final class CameraRecordingSampler {
    private double nextSampleTime;
    private double previousTime,px,py,pz,proll,pzoom,lastInputRoll,unwrappedRoll; private float pyaw,ppitch; private CameraMode pmode; private boolean initialized;
    public void reset() { nextSampleTime=0; initialized=false; }
    public boolean sample(CameraRecording recording, CameraPose pose, double roll, double zoom, CameraMode mode,
                          double elapsed, int rate, int limit) {
        double interval=1.0/Math.clamp(rate,10,120); boolean accepted=true;
        if(!initialized){initialized=true;previousTime=elapsed;px=pose.x();py=pose.y();pz=pose.z();pyaw=pose.yaw();ppitch=pose.pitch();lastInputRoll=roll;unwrappedRoll=roll;proll=unwrappedRoll;pzoom=zoom;pmode=mode;}
        else{double delta=com.cukkoo.freecamera.roll.CameraRollState.normalizeDegrees(roll-lastInputRoll);unwrappedRoll+=delta;lastInputRoll=roll;}
        while (nextSampleTime <= elapsed + 1.0E-9) {
            double span=elapsed-previousTime;double a=span<=0?0:Math.clamp((nextSampleTime-previousTime)/span,0,1);
            float iyaw=net.minecraft.util.Mth.wrapDegrees(pyaw+net.minecraft.util.Mth.wrapDegrees(pose.yaw()-pyaw)*(float)a);
            accepted &= recording.add(new CameraRecordingFrame(nextSampleTime,px+(pose.x()-px)*a,py+(pose.y()-py)*a,pz+(pose.z()-pz)*a,iyaw,ppitch+(pose.pitch()-ppitch)*(float)a,proll+(unwrappedRoll-proll)*a,pzoom+(zoom-pzoom)*a,pmode),limit);
            nextSampleTime += interval; if (!accepted) break;
        }
        previousTime=elapsed;px=pose.x();py=pose.y();pz=pose.z();pyaw=pose.yaw();ppitch=pose.pitch();proll=unwrappedRoll;pzoom=zoom;pmode=mode;
        return accepted;
    }
}
