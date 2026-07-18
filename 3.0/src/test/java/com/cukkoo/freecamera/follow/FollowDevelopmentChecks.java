package com.cukkoo.freecamera.follow;

public final class FollowDevelopmentChecks {
 public static void main(String[] args){
  FollowMotionIntegrator integrator=new FollowMotionIntegrator();double reference=Double.NaN;
  for(int fps:new int[]{10,20,30,60,144,240}){double position=0;for(int i=0;i<fps;i++)position+=(10-position)*integrator.response(1.0/fps,10);if(Double.isNaN(reference))reference=position;require(Math.abs(position-reference)<1e-10,"follow FPS drift at "+fps);}
  require(integrator.response(Double.NaN,10)==0&&integrator.response(.3,10)==0,"discontinuity accepted");
  FollowRotationSmoother rotation=new FollowRotationSmoother();float wrapped=rotation.approach(170,-170,.5);require(Math.abs(Math.abs(wrapped)-180)<1e-4,"rotation did not use shortest path");
  require(FollowProfile.values().length==4,"follow profiles missing");
  System.out.println("Follow smoothing is FPS-consistent at 10, 20, 30, 60, 144, and 240 FPS; angle and discontinuity checks passed.");
 }
 private static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
}
