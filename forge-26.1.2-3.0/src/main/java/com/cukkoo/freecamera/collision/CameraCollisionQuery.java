package com.cukkoo.freecamera.collision;

import net.minecraft.core.Direction;

interface CameraCollisionQuery {
    boolean isSweepLoaded(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            double halfExtent
    );

    boolean isVolumeFree(double x, double y, double z, double halfExtent);

    double collideAxis(
            Direction.Axis axis,
            double x, double y, double z,
            double halfExtent,
            double movement
    );

    double orbitFraction(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            double halfExtent,
            double skin
    );
}
