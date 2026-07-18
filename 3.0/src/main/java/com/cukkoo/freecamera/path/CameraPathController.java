package com.cukkoo.freecamera.path;

import com.cukkoo.freecamera.api.CameraPose;

public final class CameraPathController {
 private final CameraPathInterpolator interpolator=new CameraPathInterpolator();
 private CameraPath path;private double time,speed=1,outputRoll,outputZoom=1;private long lastNanos;
 private boolean playing,paused,loop,reverse,pingPong,output,screenSuspended,resumeAfterScreen;
 public boolean applyFrame(CameraPose pose,boolean enabled){
  if(!enabled){if(!screenSuspended){screenSuspended=true;resumeAfterScreen=playing&&!paused;if(resumeAfterScreen)pause();}lastNanos=0;return false;}
  if(screenSuspended){screenSuspended=false;if(resumeAfterScreen)resume();resumeAfterScreen=false;lastNanos=0;}
  long now=System.nanoTime();double elapsed=lastNanos==0?0:(now-lastNanos)*1e-9;lastNanos=now;return apply(pose,elapsed);
 }
 public void play(CameraPath value){path=value;playing=value!=null&&value.size()>=2;paused=false;screenSuspended=false;resumeAfterScreen=false;time=reverse?duration():0;}
 public boolean apply(CameraPose pose,double elapsed){
  output=false;if(!playing||paused||path==null)return false;if(Double.isFinite(elapsed)&&elapsed>0&&elapsed<=.25)time+=(reverse?-1:1)*elapsed*speed;double total=duration();
  if(time<0||time>total){if(pingPong){reverse=!reverse;time=Math.clamp(time,0,total);}else if(loop)time=reverse?total:0;else{time=Math.clamp(time,0,total);playing=false;}}
  double cursor=0;int segment=0;for(;segment<path.size()-1;segment++){var frame=path.get(segment);double span=frame.hold()+frame.duration();if(time<=cursor+span)break;cursor+=span;}
  if(segment>=path.size()-1){var frame=path.get(path.size()-1);interpolator.apply(path,path.size()-1,1,pose);outputRoll=frame.roll();outputZoom=frame.zoom();output=true;return true;}
  var frame=path.get(segment);var next=path.get(segment+1);double local=time-cursor;double raw=local<=frame.hold()?0:Math.clamp((local-frame.hold())/Math.max(1e-9,frame.duration()),0,1);double eased=frame.easing().apply(raw);interpolator.apply(path,segment,raw,pose);outputRoll=frame.roll()+(next.roll()-frame.roll())*eased;outputZoom=Math.clamp(frame.zoom()+(next.zoom()-frame.zoom())*eased,1,10);output=true;return true;
 }
 public double duration(){double d=0;if(path!=null)for(int i=0;i<path.size()-1;i++)d+=path.get(i).hold()+path.get(i).duration();return d;}
 public void pause(){paused=true;}public void resume(){paused=false;}public void stop(){playing=false;paused=false;screenSuspended=false;resumeAfterScreen=false;}
 public void suspendForScreen(){if(!screenSuspended){screenSuspended=true;resumeAfterScreen=playing&&!paused;if(resumeAfterScreen)pause();}lastNanos=0;}
 public void clear(){path=null;playing=false;paused=false;time=0;lastNanos=0;output=false;screenSuspended=false;resumeAfterScreen=false;}
 public void setLoop(boolean v){loop=v;}public void setReverse(boolean v){reverse=v;}public void setPingPong(boolean v){pingPong=v;}public void setSpeed(double v){speed=Math.clamp(v,.1,4);}
 public boolean isPlaying(){return playing;}public boolean isPaused(){return paused;}public boolean isScreenSuspended(){return screenSuspended;}public boolean isLoop(){return loop;}public boolean isReverse(){return reverse;}public boolean isPingPong(){return pingPong;}public double speed(){return speed;}public double time(){return time;}public String name(){return path==null?"":path.name();}public boolean hasOutput(){return output;}public double outputRoll(){return outputRoll;}public double outputZoom(){return outputZoom;}
}
