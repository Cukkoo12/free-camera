package com.cukkoo.freecamera.follow;

import net.minecraft.util.Mth;

public final class FollowProfileMath {
    private FollowProfileMath() {
    }

    public static float bearing(FollowProfile profile, float targetYaw, float framingYaw) {
        return switch (profile) {
            case CHASE -> targetYaw + framingYaw;
            case SIDE -> targetYaw + 90.0F + framingYaw;
            case FIXED_ANGLE -> framingYaw;
            case LOOK_AT -> throw new IllegalArgumentException("LOOK_AT has no target-relative bearing");
        };
    }

    public static float framingFromWorldBearing(
            FollowProfile profile,
            float worldBearing,
            float targetYaw
    ) {
        return switch (profile) {
            case CHASE -> Mth.wrapDegrees(worldBearing - targetYaw);
            case SIDE -> Mth.wrapDegrees(worldBearing - targetYaw - 90.0F);
            case FIXED_ANGLE -> Mth.wrapDegrees(worldBearing);
            case LOOK_AT -> 0.0F;
        };
    }

    public static double desiredX(
            double anchorX, double distance, double lateral, float bearing
    ) {
        double radians = Math.toRadians(bearing);
        return anchorX + Math.sin(radians) * distance + Math.cos(radians) * lateral;
    }

    public static double desiredZ(
            double anchorZ, double distance, double lateral, float bearing
    ) {
        double radians = Math.toRadians(bearing);
        return anchorZ - Math.cos(radians) * distance + Math.sin(radians) * lateral;
    }

    public static double distanceDelta(double forwardInput, double elapsed, double rate) {
        return -forwardInput * elapsed * rate;
    }

    public static double bearingDelta(double strafeInput, double elapsed, double rate) {
        return -strafeInput * elapsed * rate;
    }
}
