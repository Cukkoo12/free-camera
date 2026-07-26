package com.cukkoo.freecamera.collision;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;

final class MinecraftCameraCollisionQuery implements CameraCollisionQuery {
    private static final int MAX_CHUNKS_PER_QUERY = 36;

    private final ClientLevel level;

    MinecraftCameraCollisionQuery(ClientLevel level) {
        this.level = level;
    }

    @Override
    public boolean isSweepLoaded(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            double halfExtent
    ) {
        int minimumChunkX = floorToChunk(Math.min(fromX, toX) - halfExtent);
        int maximumChunkX = floorToChunk(Math.max(fromX, toX) + halfExtent);
        int minimumChunkZ = floorToChunk(Math.min(fromZ, toZ) - halfExtent);
        int maximumChunkZ = floorToChunk(Math.max(fromZ, toZ) + halfExtent);
        long width = (long) maximumChunkX - minimumChunkX + 1L;
        long depth = (long) maximumChunkZ - minimumChunkZ + 1L;
        if (width <= 0L || depth <= 0L || width * depth > MAX_CHUNKS_PER_QUERY) {
            return false;
        }
        for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
            for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean isVolumeFree(double x, double y, double z, double halfExtent) {
        AABB volume = volumeAt(x, y, z, halfExtent);
        return level.noBlockCollision(null, volume);
    }

    @Override
    public double collideAxis(
            Direction.Axis axis,
            double x, double y, double z,
            double halfExtent,
            double movement
    ) {
        if (movement == 0.0) {
            return 0.0;
        }
        AABB volume = volumeAt(x, y, z, halfExtent);
        double movementX = axis == Direction.Axis.X ? movement : 0.0;
        double movementY = axis == Direction.Axis.Y ? movement : 0.0;
        double movementZ = axis == Direction.Axis.Z ? movement : 0.0;
        AABB sweep = volume.expandTowards(movementX, movementY, movementZ);
        return Shapes.collide(axis, volume, level.getBlockCollisions(null, sweep), movement);
    }

    @Override
    public double orbitFraction(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            double halfExtent,
            double skin
    ) {
        double minimum = 1.0;
        minimum = Math.min(minimum, rayFraction(fromX, fromY, fromZ, toX, toY, toZ, skin));
        minimum = Math.min(minimum, rayFraction(fromX - halfExtent, fromY - halfExtent,
                fromZ - halfExtent, toX - halfExtent, toY - halfExtent, toZ - halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX - halfExtent, fromY - halfExtent,
                fromZ + halfExtent, toX - halfExtent, toY - halfExtent, toZ + halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX - halfExtent, fromY + halfExtent,
                fromZ - halfExtent, toX - halfExtent, toY + halfExtent, toZ - halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX - halfExtent, fromY + halfExtent,
                fromZ + halfExtent, toX - halfExtent, toY + halfExtent, toZ + halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX + halfExtent, fromY - halfExtent,
                fromZ - halfExtent, toX + halfExtent, toY - halfExtent, toZ - halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX + halfExtent, fromY - halfExtent,
                fromZ + halfExtent, toX + halfExtent, toY - halfExtent, toZ + halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX + halfExtent, fromY + halfExtent,
                fromZ - halfExtent, toX + halfExtent, toY + halfExtent, toZ - halfExtent, skin));
        minimum = Math.min(minimum, rayFraction(fromX + halfExtent, fromY + halfExtent,
                fromZ + halfExtent, toX + halfExtent, toY + halfExtent, toZ + halfExtent, skin));
        return minimum;
    }

    private double rayFraction(
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ,
            double skin
    ) {
        Vec3 from = new Vec3(fromX, fromY, fromZ);
        Vec3 to = new Vec3(toX, toY, toZ);
        BlockHitResult hit = level.clip(new ClipContext(
                from,
                to,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                CollisionContext.empty()
        ));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return 1.0;
        }
        double totalX = toX - fromX;
        double totalY = toY - fromY;
        double totalZ = toZ - fromZ;
        double totalLength = Math.sqrt(totalX * totalX + totalY * totalY + totalZ * totalZ);
        if (totalLength <= 1.0E-9 || hit.isInside()) {
            return 0.0;
        }
        Vec3 location = hit.getLocation();
        double hitX = location.x - fromX;
        double hitY = location.y - fromY;
        double hitZ = location.z - fromZ;
        double hitDistance = Math.sqrt(hitX * hitX + hitY * hitY + hitZ * hitZ);
        return Math.clamp((hitDistance - skin) / totalLength, 0.0, 1.0);
    }

    private static AABB volumeAt(double x, double y, double z, double halfExtent) {
        return new AABB(
                x - halfExtent, y - halfExtent, z - halfExtent,
                x + halfExtent, y + halfExtent, z + halfExtent
        );
    }

    private static int floorToChunk(double coordinate) {
        return Math.floorDiv((int) Math.floor(coordinate), 16);
    }
}
