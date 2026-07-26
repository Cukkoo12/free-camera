package com.cukkoo.freecamera.collision;

import net.minecraft.core.Direction;

public final class FreeCameraCollisionResolver {
    public static final double CAMERA_HALF_EXTENT = 0.15;
    public static final double SURFACE_SKIN = 0.01;
    private static final double COLLISION_EPSILON = 1.0E-7;

    private final CameraCollisionResult result = new CameraCollisionResult();
    private double surfaceSkin=SURFACE_SKIN;void configure(double margin){surfaceSkin=Math.clamp(margin,.001,.1);}

    CameraCollisionResult resolve(
            CameraCollisionQuery world,
            double fromX, double fromY, double fromZ,
            double desiredX, double desiredY, double desiredZ
    ) {
        if (!world.isSweepLoaded(
                fromX, fromY, fromZ,
                desiredX, desiredY, desiredZ,
                CAMERA_HALF_EXTENT
        )) {
            return result.set(fromX, fromY, fromZ, true, true);
        }

        double movementX = desiredX - fromX;
        double movementY = desiredY - fromY;
        double movementZ = desiredZ - fromZ;
        double resolvedX = fromX;
        double resolvedY = fromY;
        double resolvedZ = fromZ;

        double clippedY = withSkin(world.collideAxis(
                Direction.Axis.Y, resolvedX, resolvedY, resolvedZ,
                CAMERA_HALF_EXTENT, movementY
        ), movementY);
        resolvedY += clippedY;

        boolean xFirst = Math.abs(movementX) >= Math.abs(movementZ);
        if (xFirst) {
            double clippedX = withSkin(world.collideAxis(
                    Direction.Axis.X, resolvedX, resolvedY, resolvedZ,
                    CAMERA_HALF_EXTENT, movementX
            ), movementX);
            resolvedX += clippedX;
            double clippedZ = withSkin(world.collideAxis(
                    Direction.Axis.Z, resolvedX, resolvedY, resolvedZ,
                    CAMERA_HALF_EXTENT, movementZ
            ), movementZ);
            resolvedZ += clippedZ;
        } else {
            double clippedZ = withSkin(world.collideAxis(
                    Direction.Axis.Z, resolvedX, resolvedY, resolvedZ,
                    CAMERA_HALF_EXTENT, movementZ
            ), movementZ);
            resolvedZ += clippedZ;
            double clippedX = withSkin(world.collideAxis(
                    Direction.Axis.X, resolvedX, resolvedY, resolvedZ,
                    CAMERA_HALF_EXTENT, movementX
            ), movementX);
            resolvedX += clippedX;
        }

        boolean obstructed = Math.abs(resolvedX - desiredX) > COLLISION_EPSILON
                || Math.abs(resolvedY - desiredY) > COLLISION_EPSILON
                || Math.abs(resolvedZ - desiredZ) > COLLISION_EPSILON;
        return result.set(resolvedX, resolvedY, resolvedZ, obstructed, false);
    }

    private double withSkin(double clipped, double requested) {
        if (Math.abs(clipped - requested) <= COLLISION_EPSILON) {
            return clipped;
        }
        if (clipped > 0.0) {
            return Math.max(0.0, clipped - surfaceSkin);
        }
        if (clipped < 0.0) {
            return Math.min(0.0, clipped + surfaceSkin);
        }
        return 0.0;
    }
}
