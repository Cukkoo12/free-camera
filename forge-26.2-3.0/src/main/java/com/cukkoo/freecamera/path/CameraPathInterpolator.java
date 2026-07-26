package com.cukkoo.freecamera.path;

import com.cukkoo.freecamera.api.CameraPose;
import net.minecraft.util.Mth;

public final class CameraPathInterpolator {
 public void apply(CameraPath path,int segment,double alpha,CameraPose output){
  int last=path.size()-1;if(segment>=last){var frame=path.get(last);output.set(frame.x(),frame.y(),frame.z(),frame.yaw(),frame.pitch());return;}
  var a=path.get(segment);var b=path.get(segment+1);double time=a.easing().apply(Math.clamp(alpha,0,1));double x,y,z;
  if(path.interpolation()==PathInterpolation.SMOOTH&&path.size()>2){
   var before=path.get(Math.max(0,segment-1));var after=path.get(Math.min(last,segment+2));
   x=boundedCat(before.x(),a.x(),b.x(),after.x(),time);y=boundedCat(before.y(),a.y(),b.y(),after.y(),time);z=boundedCat(before.z(),a.z(),b.z(),after.z(),time);
   if(!finite(x,y,z)){x=lerp(a.x(),b.x(),time);y=lerp(a.y(),b.y(),time);z=lerp(a.z(),b.z(),time);}
  }else{x=lerp(a.x(),b.x(),time);y=lerp(a.y(),b.y(),time);z=lerp(a.z(),b.z(),time);}
  output.set(x,y,z,Mth.wrapDegrees(a.yaw()+Mth.wrapDegrees(b.yaw()-a.yaw())*(float)time),Mth.clamp(a.pitch()+(b.pitch()-a.pitch())*(float)time,-90,90));
 }
 private static double lerp(double a,double b,double time){return a+(b-a)*time;}
 private static double boundedCat(double p0,double p1,double p2,double p3,double time){double squared=time*time,cubed=squared*time;double value=.5*((2*p1)+(-p0+p2)*time+(2*p0-5*p1+4*p2-p3)*squared+(-p0+3*p1-3*p2+p3)*cubed);return Double.isFinite(value)?Math.clamp(value,Math.min(p1,p2),Math.max(p1,p2)):value;}
 private static boolean finite(double x,double y,double z){return Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z);}
}
