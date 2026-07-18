package com.cukkoo.freecamera.path;
public record CameraKeyframe(double x,double y,double z,float yaw,float pitch,double roll,double zoom,
                             double duration,PathEasing easing,double hold){
    public CameraKeyframe{duration=valid(duration,1);hold=valid(hold,0);zoom=Double.isFinite(zoom)?Math.clamp(zoom,1,10):1;pitch=Math.clamp(pitch,-90,90);}
    private static double valid(double v,double fallback){return Double.isFinite(v)&&v>=0?Math.min(v,3600):fallback;}
}
