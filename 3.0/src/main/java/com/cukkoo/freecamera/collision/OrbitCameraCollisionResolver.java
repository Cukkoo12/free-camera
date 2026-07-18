package com.cukkoo.freecamera.collision;

public final class OrbitCameraCollisionResolver {
    public static final double CAMERA_HALF_EXTENT = 0.15;
    public static final double SURFACE_SKIN = 0.01;

    private final CameraCollisionResult result = new CameraCollisionResult();
    private double surfaceSkin=SURFACE_SKIN;void configure(double margin){surfaceSkin=Math.clamp(margin,.001,.1);}

    CameraCollisionResult resolve(
            CameraCollisionQuery world,
            double anchorX, double anchorY, double anchorZ,
            double desiredX, double desiredY, double desiredZ
    ) {
        if (!world.isSweepLoaded(
                anchorX, anchorY, anchorZ,
                desiredX, desiredY, desiredZ,
                CAMERA_HALF_EXTENT
        )) {
            return result.set(desiredX, desiredY, desiredZ, true, true);
        }
        double fraction = world.orbitFraction(
                anchorX, anchorY, anchorZ,
                desiredX, desiredY, desiredZ,
                CAMERA_HALF_EXTENT,
                surfaceSkin
        );
        return result.set(
                anchorX + (desiredX - anchorX) * fraction,
                anchorY + (desiredY - anchorY) * fraction,
                anchorZ + (desiredZ - anchorZ) * fraction,
                fraction < 1.0 - 1.0E-7,
                false
        );
    }
}
