package com.cukkoo.freecamera.recording;

import com.cukkoo.freecamera.api.CameraPose;

public final class CameraPlaybackController {
    private final CameraTrackInterpolator interpolator=new CameraTrackInterpolator();
    private CameraRecording recording; private double time; private double speed=1,outputRoll,outputZoom=1; private boolean playing,paused,loop,reverse,output;
    public void play(CameraRecording r){recording=r;time=reverse&&r.size()>0?r.get(r.size()-1).time():0;playing=r.size()>0;paused=false;}
    public boolean apply(CameraPose pose,double elapsed){
        output=false;if(!playing||paused||recording==null)return false; if(Double.isFinite(elapsed)&&elapsed>0&&elapsed<=.25)time+=(reverse?-1:1)*elapsed*speed;
        double duration=recording.get(recording.size()-1).time(); if(time<0||time>duration){if(loop)time=reverse?duration:0;else{time=Math.clamp(time,0,duration);playing=false;}}
        int hi=1; while(hi<recording.size()&&recording.get(hi).time()<time)hi++; if(hi>=recording.size())hi=recording.size()-1;
        int lo=Math.max(0,hi-1); var a=recording.get(lo);var b=recording.get(hi);double span=b.time()-a.time();
        double alpha=span<=0?0:(time-a.time())/span;interpolator.apply(a,b,alpha,pose);outputRoll=interpolator.roll(a,b,alpha);outputZoom=interpolator.zoom(a,b,alpha);output=true;return true;
    }
    public void pause(){paused=true;} public void resume(){paused=false;} public void stop(){playing=false;paused=false;}
    public void setLoop(boolean v){loop=v;} public void setReverse(boolean v){reverse=v;} public void setSpeed(double v){speed=Math.clamp(v,.1,4);}
    public boolean isPlaying(){return playing;} public double time(){return time;} public boolean hasOutput(){return output;}public double outputRoll(){return outputRoll;}public double outputZoom(){return outputZoom;}
    public boolean isPaused(){return paused;}public boolean isLoop(){return loop;}public boolean isReverse(){return reverse;}public double speed(){return speed;}public double duration(){return recording==null||recording.size()==0?0:recording.get(recording.size()-1).time();}public String name(){return recording==null?"":recording.name();}
}
