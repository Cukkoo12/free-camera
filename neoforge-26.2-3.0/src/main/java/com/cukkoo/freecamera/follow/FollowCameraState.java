package com.cukkoo.freecamera.follow;

import net.minecraft.world.entity.Entity;

public final class FollowCameraState {
    private FollowProfile profile = FollowProfile.CHASE;
    private Entity target;
    private double distance = 6.0;
    private double height = 2.0;
    private double lateral;
    private float framingYaw;
    private float framingPitch = 12.0F;

    public FollowProfile profile() { return profile; }
    public Entity target() { return target; }
    public double distance() { return distance; }
    public double height() { return height; }
    public double lateral() { return lateral; }
    public float framingYaw() { return framingYaw; }
    public float framingPitch() { return framingPitch; }
    public void setProfile(FollowProfile profile) { this.profile = profile; }
    public void configure(double distance,double height,double lateral){this.distance=Math.clamp(distance,1,64);this.height=Math.clamp(height,-16,32);this.lateral=Math.clamp(lateral,-32,32);}
    public void setTarget(Entity target) { this.target = target; }
    public void adjustDistance(double delta) { distance = Math.clamp(distance + delta, 1.0, 64.0); }
    public void adjustOffsets(double side, double vertical) {
        lateral = Math.clamp(lateral + side, -32.0, 32.0);
        height = Math.clamp(height + vertical, -16.0, 32.0);
    }
    public void adjustBearing(double degrees) {
        framingYaw = net.minecraft.util.Mth.wrapDegrees(framingYaw + (float) degrees);
    }
    public void setBearing(float degrees) {
        framingYaw = net.minecraft.util.Mth.wrapDegrees(degrees);
    }
    public void rebuildFraming(double distance, double height, float framingYaw) {
        this.distance = Math.clamp(distance, 1.0, 64.0);
        this.height = Math.clamp(height, -16.0, 32.0);
        this.lateral = 0.0;
        this.framingYaw = net.minecraft.util.Mth.wrapDegrees(framingYaw);
    }
    public void rotate(float yaw, float pitch) {
        framingYaw = net.minecraft.util.Mth.wrapDegrees(framingYaw + yaw);
        framingPitch = Math.clamp(framingPitch + pitch, -85.0F, 85.0F);
    }
    public void clear() { target = null; framingYaw = 0.0F; framingPitch = 12.0F; }
}
