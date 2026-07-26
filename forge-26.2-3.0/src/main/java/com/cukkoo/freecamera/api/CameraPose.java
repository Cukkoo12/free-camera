package com.cukkoo.freecamera.api;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class CameraPose {
    private double x;
    private double y;
    private double z;
    private float yaw;
    private float pitch;

    public CameraPose(double x, double y, double z, float yaw, float pitch) {
        set(x, y, z, yaw, pitch);
    }

    public void set(double x, double y, double z, float yaw, float pitch) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = Mth.wrapDegrees(yaw);
        this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
    }

    public void set(Vec3 position, float yaw, float pitch) {
        set(position.x, position.y, position.z, yaw, pitch);
    }

    public void rotate(double yawInput, double pitchInput) {
        yaw = Mth.wrapDegrees(yaw + (float) yawInput * 0.15F);
        pitch = Mth.clamp(pitch + (float) pitchInput * 0.15F, -90.0F, 90.0F);
    }

    public void setRotation(float yaw, float pitch) {
        this.yaw = Mth.wrapDegrees(yaw);
        this.pitch = Mth.clamp(pitch, -90.0F, 90.0F);
    }

    public void setPosition(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void copyFrom(CameraPose other) {
        set(other.x, other.y, other.z, other.yaw, other.pitch);
    }

    public void translate(double deltaX, double deltaY, double deltaZ) {
        x += deltaX;
        y += deltaY;
        z += deltaZ;
    }

    public void clear() {
        x = 0.0;
        y = 0.0;
        z = 0.0;
        yaw = 0.0F;
        pitch = 0.0F;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }
}
