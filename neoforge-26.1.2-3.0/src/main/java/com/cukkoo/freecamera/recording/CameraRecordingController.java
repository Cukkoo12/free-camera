package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;

public final class CameraRecordingController {
    private final CameraRecordingSampler sampler=new CameraRecordingSampler();
    private final CameraPlaybackController playback=new CameraPlaybackController();
    private CameraRecording current,last; private long lastRecordNanos,lastPlaybackNanos; private double recordElapsed; private boolean recording,suspended,resumePlaybackAfterSuspension;
    public void toggleRecording(){if(recording)stopRecording();else startRecording();}
    public void startRecording(){current=new CameraRecording();sampler.reset();lastRecordNanos=0;recordElapsed=0;recording=true;}
    public void stopRecording(){if(recording){last=current;recording=false;}}
    public boolean sample(CameraPose pose,double roll,double zoom,CameraMode mode,int rate,int maxSeconds,int maximumSamples){if(recording&&!suspended){long now=System.nanoTime();double delta=lastRecordNanos==0?0:(now-lastRecordNanos)*1e-9;lastRecordNanos=now;if(Double.isFinite(delta)&&delta>0&&delta<=.25)recordElapsed+=delta;int limit=Math.min(maximumSamples,rate*maxSeconds+1);if(recordElapsed>maxSeconds||!sampler.sample(current,pose,roll,zoom,mode,recordElapsed,rate,limit)){stopRecording();return true;}}return false;}
    public boolean applyPlayback(CameraPose pose,boolean inputEnabled){if(!inputEnabled){suspend();return false;}if(suspended){suspended=false;if(resumePlaybackAfterSuspension)playback.resume();resumePlaybackAfterSuspension=false;lastPlaybackNanos=0;}long now=System.nanoTime();double e=lastPlaybackNanos==0?0:(now-lastPlaybackNanos)*1e-9;lastPlaybackNanos=now;return playback.apply(pose,e);}
    public void playLast(){if(last!=null){playback.play(last);lastPlaybackNanos=0;}} public void togglePlayback(){if(playback.isPlaying())playback.stop();else playLast();}
    public void suspend(){if(!suspended){suspended=true;resumePlaybackAfterSuspension=playback.isPlaying()&&!playback.isPaused();if(resumePlaybackAfterSuspension)playback.pause();lastPlaybackNanos=0;lastRecordNanos=0;}}
    public void clear(){if(recording&&current!=null)last=current;recording=false;current=null;playback.stop();lastPlaybackNanos=0;suspended=false;resumePlaybackAfterSuspension=false;}
    public CameraRecording last(){return last;} public boolean isRecording(){return recording;} public CameraPlaybackController playback(){return playback;}
    public void setLast(CameraRecording recording){last=recording;}
    public double recordingElapsed(){return recordElapsed;}public int recordingSamples(){return current==null?0:current.size();}public boolean isScreenSuspended(){return suspended;}
}
