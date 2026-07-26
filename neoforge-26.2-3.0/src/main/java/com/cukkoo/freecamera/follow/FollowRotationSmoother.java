package com.cukkoo.freecamera.follow;

import net.minecraft.util.Mth;

public final class FollowRotationSmoother {
    public float approach(float current, float target, double response) {
        return Mth.wrapDegrees(current + Mth.wrapDegrees(target - current) * (float) response);
    }
}
