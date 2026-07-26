package com.cukkoo.freecamera.follow;

import com.cukkoo.freecamera.api.CameraPose;
import com.cukkoo.freecamera.input.CameraInputSnapshot;

public final class FollowMotionIntegrator {
    private double velocityX;
    private double velocityY;
    private double velocityZ;

    public double response(double elapsed, double frequency) {
        return !accepted(elapsed) ? 0.0 : 1.0 - Math.exp(-frequency * elapsed);
    }

    public void moveCameraRelative(
            CameraPose pose,
            CameraInputSnapshot input,
            double elapsed,
            double speed,
            double response
    ) {
        if (!accepted(elapsed)) {
            clear();
            return;
        }
        double yaw = Math.toRadians(pose.yaw());
        double pitch = Math.toRadians(pose.pitch());
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double cosPitch = Math.cos(pitch);
        double sinPitch = Math.sin(pitch);
        double targetX = input.forward() * -sinYaw * cosPitch - input.strafe() * cosYaw;
        double targetY = input.forward() * -sinPitch + input.vertical();
        double targetZ = input.forward() * cosYaw * cosPitch - input.strafe() * sinYaw;
        double lengthSquared = targetX * targetX + targetY * targetY + targetZ * targetZ;
        if (lengthSquared > 0.0 && Double.isFinite(lengthSquared)) {
            double scale = speed / Math.sqrt(lengthSquared);
            targetX *= scale;
            targetY *= scale;
            targetZ *= scale;
        } else {
            targetX = targetY = targetZ = 0.0;
        }

        double frequency = Math.max(0.1, response);
        double decay = Math.exp(-frequency * elapsed);
        double factor = (1.0 - decay) / frequency;
        double dx = targetX * elapsed + (velocityX - targetX) * factor;
        double dy = targetY * elapsed + (velocityY - targetY) * factor;
        double dz = targetZ * elapsed + (velocityZ - targetZ) * factor;
        velocityX = targetX + (velocityX - targetX) * decay;
        velocityY = targetY + (velocityY - targetY) * decay;
        velocityZ = targetZ + (velocityZ - targetZ) * decay;
        pose.translate(dx, dy, dz);
    }

    public void clear() {
        velocityX = velocityY = velocityZ = 0.0;
    }

    double velocityXForDevelopmentCheck() {
        return velocityX;
    }

    private static boolean accepted(double elapsed) {
        return Double.isFinite(elapsed) && elapsed > 0.0 && elapsed <= 0.25;
    }
}
