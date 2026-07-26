package com.cukkoo.freecamera.collision;

import com.cukkoo.freecamera.api.CameraMode;
import com.cukkoo.freecamera.api.CameraPose;
import net.minecraft.core.Direction;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CameraCollisionDevelopmentChecks {
    private static final double EPSILON = 1.0E-8;
    private static final int[] FRAME_RATES = {10, 20, 30, 60, 144, 240};

    private CameraCollisionDevelopmentChecks() {
    }

    public static void main(String[] args) {
        verifyDefaultsAndOffQueriesNothing();
        verifyMinecraftInvalidationTarget();
        verifyFreeWallTunnellingAndSliding();
        verifyFreeSoftRecovery();
        verifyThinBlocksCornersPlantsAndLiquids();
        verifyStartingInsideDoesNotOscillate();
        verifyDynamicObstructionAndUnloadedBoundary();
        verifyOrbitRetractionAndRecovery();
        verifyVanillaRecoveryIsImmediate();
        verifyTripodAndVisibleModeTransitions();
        verifySuspensionAndCleanup();
        verifyRecoveryAcrossFrameRates();
        System.out.println("Camera collision geometry, recovery, mode, and safety checks passed.");
    }

    private static void verifyDefaultsAndOffQueriesNothing() {
        require(CameraCollisionMode.defaultFor(CameraMode.FREE_CAMERA) == CameraCollisionMode.OFF,
                "Free collision did not default to OFF");
        require(CameraCollisionMode.defaultFor(CameraMode.ORBIT) == CameraCollisionMode.SOFT,
                "Orbit collision did not default to SOFT");
        TestWorld world = new TestWorld();
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(12.0, 4.0, -7.0);
        CameraPose rendered = pose(0.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.OFF,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requirePose(rendered, 12.0, 4.0, -7.0, "OFF restricted movement");
        require(world.queryCount == 0, "OFF performed a collision query");
    }

    private static void verifyMinecraftInvalidationTarget() {
        try {
            ClientLevel.class.getDeclaredMethod(
                    "sendBlockUpdated",
                    BlockPos.class,
                    BlockState.class,
                    BlockState.class,
                    int.class
            );
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Minecraft block-update invalidation target changed", exception);
        }
    }

    private static void verifyFreeWallTunnellingAndSliding() {
        TestWorld world = new TestWorld();
        world.add(2.0, -4.0, -10.0, 3.0, 4.0, 10.0);

        CameraPose wallStop = resolveFree(world, 0.0, 0.0, 0.0, 5.0, 0.0, 0.0);
        require(wallStop.x() < 2.0 - FreeCameraCollisionResolver.CAMERA_HALF_EXTENT,
                "Free camera entered the wall");
        requireClose(wallStop.x(), 1.84, 1.0E-6, "surface skin was not preserved");

        CameraPose highSpeed = resolveFree(world, -100.0, 0.0, 0.0, 100.0, 0.0, 0.0);
        require(highSpeed.x() < 2.0, "high-speed movement tunnelled through a wall");

        CameraPose diagonal = resolveFree(world, 0.0, 0.0, 0.0, 5.0, 0.0, 3.0);
        require(diagonal.x() < 2.0 && close(diagonal.z(), 3.0),
                "diagonal wall sliding entered the wall or lost tangent motion");
    }

    private static void verifyFreeSoftRecovery() {
        TestWorld world = new TestWorld();
        world.add(2.0, -2.0, -2.0, 3.0, 2.0, 2.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(5.0, 0.0, 0.0);
        CameraPose rendered = pose(0.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.SOFT,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        double stopped = rendered.x();
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.SOFT,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requireClose(rendered.x(), stopped, EPSILON,
                "Free SOFT recovery advanced through a remaining wall");
        world.clear();
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.SOFT,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        require(rendered.x() > stopped && rendered.x() < desired.x(),
                "Free SOFT recovery was not smooth after obstruction cleared");
    }

    private static void verifyThinBlocksCornersPlantsAndLiquids() {
        TestWorld thin = new TestWorld();
        thin.add(1.0, -1.0, -1.0, 1.0625, 1.0, 1.0);
        CameraPose thinStop = resolveFree(thin, 0.0, 0.0, 0.0, 4.0, 0.0, 0.0);
        require(thinStop.x() < 1.0, "thin collision shape was missed");

        TestWorld corner = new TestWorld();
        corner.add(1.0, -1.0, 1.0, 2.0, 1.0, 2.0);
        CameraPose cornerStop = resolveFree(corner, 0.0, 0.0, 0.0, 3.0, 0.0, 3.0);
        require(!corner.intersects(
                        cornerStop.x(), cornerStop.y(), cornerStop.z(),
                        FreeCameraCollisionResolver.CAMERA_HALF_EXTENT
                ), "camera-volume corner entered a collision shape");

        TestWorld nonCollidable = new TestWorld();
        // Plants and liquids have no block collision shape, so neither is added to this shape world.
        CameraPose ignored = resolveFree(nonCollidable, 0.0, 0.0, 0.0, 6.0, 0.0, 0.0);
        requirePose(ignored, 6.0, 0.0, 0.0, "plant or liquid blocked the camera");
    }

    private static void verifyStartingInsideDoesNotOscillate() {
        TestWorld world = new TestWorld();
        world.add(-1.0, -1.0, -1.0, 1.0, 1.0, 1.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(0.1, 0.0, 0.0);
        CameraPose rendered = pose(0.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requirePose(rendered, 0.0, 0.0, 0.0, "inside-block handling snapped unpredictably");
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requirePose(rendered, 0.0, 0.0, 0.0, "inside-block handling oscillated");

        desired.setPosition(2.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requirePose(rendered, 2.0, 0.0, 0.0, "movement could not exit an obstruction");
    }

    private static void verifyDynamicObstructionAndUnloadedBoundary() {
        TestWorld world = new TestWorld();
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(1.0, 0.0, 0.0);
        CameraPose rendered = pose(0.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        desired.setPosition(1.5, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        world.add(1.4, -1.0, -1.0, 1.6, 1.0, 1.0);
        resolver.markWorldCollisionDirty();
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        require(rendered.x() < 1.4 - FreeCameraCollisionResolver.CAMERA_HALF_EXTENT,
                "new obstruction did not retract to a safe position");

        double safeX = rendered.x();
        world.loaded = false;
        desired.setPosition(20.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requireClose(rendered.x(), safeX, EPSILON,
                "unloaded collision boundary did not preserve the safe position");
    }

    private static void verifyOrbitRetractionAndRecovery() {
        TestWorld world = new TestWorld();
        world.add(-2.0, -2.0, 2.0, 2.0, 2.0, 3.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(0.0, 0.0, 5.0);
        CameraPose rendered = pose(0.0, 0.0, 5.0);
        resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / 60.0);
        double retracted = rendered.z();
        require(retracted < 2.0, "Orbit did not retract before the wall");

        resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / 60.0);
        require(rendered.z() <= retracted + EPSILON,
                "SOFT recovery advanced through a remaining wall");

        world.clear();
        resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / 60.0);
        require(rendered.z() > retracted && rendered.z() < 5.0,
                "Orbit did not smoothly restore requested radius");
        double previous = rendered.z();
        for (int tick = 0; tick < 240; tick++) {
            resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / 60.0);
            require(rendered.z() + EPSILON >= previous && rendered.z() <= 5.0 + EPSILON,
                    "SOFT recovery reversed or overshot");
            previous = rendered.z();
        }
        requireClose(rendered.z(), 5.0, 1.0E-6, "Orbit did not restore desired radius");
    }

    private static void verifyVanillaRecoveryIsImmediate() {
        TestWorld world = new TestWorld();
        world.add(-2.0, -2.0, 2.0, 2.0, 2.0, 3.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(0.0, 0.0, 5.0);
        CameraPose rendered = pose(0.0, 0.0, 5.0);
        resolveOrbit(resolver, world, CameraCollisionMode.VANILLA, desired, rendered, 1.0 / 60.0);
        world.clear();
        resolveOrbit(resolver, world, CameraCollisionMode.VANILLA, desired, rendered, 1.0 / 60.0);
        requireClose(rendered.z(), 5.0, EPSILON, "VANILLA recovery was smoothed");
    }

    private static void verifyTripodAndVisibleModeTransitions() {
        TestWorld world = new TestWorld();
        world.add(-2.0, -2.0, 2.0, 2.0, 2.0, 3.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(0.0, 0.0, 5.0);
        CameraPose rendered = pose(0.0, 0.0, 5.0);
        resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / 60.0);
        double visibleZ = rendered.z();

        desired.copyFrom(rendered);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.OFF,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requireClose(rendered.z(), visibleZ, EPSILON,
                "Orbit to Free used the hidden desired position");

        int queriesBeforeTripod = world.queryCount;
        desired.set(7.0, 8.0, 9.0, 33.0F, -12.0F);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.TRIPOD, CameraCollisionMode.SOFT,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        requirePose(rendered, 7.0, 8.0, 9.0, "Tripod pose was collision-adjusted");
        require(world.queryCount == queriesBeforeTripod, "Tripod queried collision");
    }

    private static void verifySuspensionAndCleanup() {
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        TestWorld world = new TestWorld();
        CameraPose desired = pose(3.0, 4.0, 5.0);
        CameraPose rendered = pose(0.0, 0.0, 0.0);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        resolver.suspend();
        requirePose(rendered, 3.0, 4.0, 5.0,
                "screen/radial suspension changed resolved position");
        resolver.clear();
        require(!resolver.state().hasSafePosition() && !resolver.state().isObstructed(),
                "cleanup retained collision state");
    }

    private static void verifyRecoveryAcrossFrameRates() {
        double reference = simulateRecovery(FRAME_RATES[0]);
        for (int index = 1; index < FRAME_RATES.length; index++) {
            double result = simulateRecovery(FRAME_RATES[index]);
            requireClose(result, reference, 1.0E-9,
                    "SOFT recovery changed at " + FRAME_RATES[index] + " FPS");
        }
    }

    private static double simulateRecovery(int fps) {
        TestWorld world = new TestWorld();
        world.add(-2.0, -2.0, 2.0, 2.0, 2.0, 3.0);
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(0.0, 0.0, 5.0);
        CameraPose rendered = pose(0.0, 0.0, 5.0);
        resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / fps);
        world.clear();
        for (int frame = 0; frame < fps; frame++) {
            resolveOrbit(resolver, world, CameraCollisionMode.SOFT, desired, rendered, 1.0 / fps);
        }
        return rendered.z();
    }

    private static CameraPose resolveFree(
            TestWorld world,
            double fromX, double fromY, double fromZ,
            double toX, double toY, double toZ
    ) {
        CameraCollisionResolver resolver = new CameraCollisionResolver();
        CameraPose desired = pose(toX, toY, toZ);
        CameraPose rendered = pose(fromX, fromY, fromZ);
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.FREE_CAMERA, CameraCollisionMode.VANILLA,
                desired, rendered, 0.0, 0.0, 0.0, 1.0 / 60.0
        );
        return rendered;
    }

    private static void resolveOrbit(
            CameraCollisionResolver resolver,
            TestWorld world,
            CameraCollisionMode mode,
            CameraPose desired,
            CameraPose rendered,
            double elapsed
    ) {
        resolver.resolveForDevelopmentCheck(
                world, CameraMode.ORBIT, mode,
                desired, rendered, 0.0, 0.0, 0.0, elapsed
        );
    }

    private static CameraPose pose(double x, double y, double z) {
        return new CameraPose(x, y, z, 0.0F, 0.0F);
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) <= EPSILON;
    }

    private static void requireClose(double actual, double expected, double tolerance, String message) {
        require(Math.abs(actual - expected) <= tolerance,
                message + ": expected " + expected + ", got " + actual);
    }

    private static void requirePose(CameraPose pose, double x, double y, double z, String message) {
        require(close(pose.x(), x) && close(pose.y(), y) && close(pose.z(), z), message);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class TestWorld implements CameraCollisionQuery {
        private static final int MAX_BOXES = 16;
        private final double[] minimumX = new double[MAX_BOXES];
        private final double[] minimumY = new double[MAX_BOXES];
        private final double[] minimumZ = new double[MAX_BOXES];
        private final double[] maximumX = new double[MAX_BOXES];
        private final double[] maximumY = new double[MAX_BOXES];
        private final double[] maximumZ = new double[MAX_BOXES];
        private int size;
        private int queryCount;
        private boolean loaded = true;

        void add(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            minimumX[size] = minX;
            minimumY[size] = minY;
            minimumZ[size] = minZ;
            maximumX[size] = maxX;
            maximumY[size] = maxY;
            maximumZ[size] = maxZ;
            size++;
        }

        void clear() {
            size = 0;
        }

        @Override
        public boolean isSweepLoaded(
                double fromX, double fromY, double fromZ,
                double toX, double toY, double toZ,
                double halfExtent
        ) {
            queryCount++;
            return loaded;
        }

        @Override
        public boolean isVolumeFree(double x, double y, double z, double halfExtent) {
            queryCount++;
            return !intersects(x, y, z, halfExtent);
        }

        boolean intersects(double x, double y, double z, double halfExtent) {
            for (int index = 0; index < size; index++) {
                if (x + halfExtent > minimumX[index] && x - halfExtent < maximumX[index]
                        && y + halfExtent > minimumY[index] && y - halfExtent < maximumY[index]
                        && z + halfExtent > minimumZ[index] && z - halfExtent < maximumZ[index]) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public double collideAxis(
                Direction.Axis axis,
                double x, double y, double z,
                double halfExtent,
                double movement
        ) {
            queryCount++;
            double clipped = movement;
            for (int index = 0; index < size; index++) {
                double boxMinX = x - halfExtent;
                double boxMaxX = x + halfExtent;
                double boxMinY = y - halfExtent;
                double boxMaxY = y + halfExtent;
                double boxMinZ = z - halfExtent;
                double boxMaxZ = z + halfExtent;
                if (axis == Direction.Axis.X
                        && overlaps(boxMinY, boxMaxY, minimumY[index], maximumY[index])
                        && overlaps(boxMinZ, boxMaxZ, minimumZ[index], maximumZ[index])) {
                    clipped = clip(clipped, boxMinX, boxMaxX, minimumX[index], maximumX[index]);
                } else if (axis == Direction.Axis.Y
                        && overlaps(boxMinX, boxMaxX, minimumX[index], maximumX[index])
                        && overlaps(boxMinZ, boxMaxZ, minimumZ[index], maximumZ[index])) {
                    clipped = clip(clipped, boxMinY, boxMaxY, minimumY[index], maximumY[index]);
                } else if (axis == Direction.Axis.Z
                        && overlaps(boxMinX, boxMaxX, minimumX[index], maximumX[index])
                        && overlaps(boxMinY, boxMaxY, minimumY[index], maximumY[index])) {
                    clipped = clip(clipped, boxMinZ, boxMaxZ, minimumZ[index], maximumZ[index]);
                }
            }
            return clipped;
        }

        @Override
        public double orbitFraction(
                double fromX, double fromY, double fromZ,
                double toX, double toY, double toZ,
                double halfExtent,
                double skin
        ) {
            queryCount++;
            double fraction = 1.0;
            double totalX = toX - fromX;
            double totalY = toY - fromY;
            double totalZ = toZ - fromZ;
            double length = Math.sqrt(totalX * totalX + totalY * totalY + totalZ * totalZ);
            for (int index = 0; index < size; index++) {
                double hit = rayBoxFraction(
                        fromX, fromY, fromZ, totalX, totalY, totalZ,
                        minimumX[index] - halfExtent, minimumY[index] - halfExtent,
                        minimumZ[index] - halfExtent, maximumX[index] + halfExtent,
                        maximumY[index] + halfExtent, maximumZ[index] + halfExtent
                );
                if (hit >= 0.0) {
                    fraction = Math.min(fraction, Math.max(0.0, hit - skin / length));
                }
            }
            return fraction;
        }

        private static double rayBoxFraction(
                double originX, double originY, double originZ,
                double deltaX, double deltaY, double deltaZ,
                double minX, double minY, double minZ,
                double maxX, double maxY, double maxZ
        ) {
            double near = 0.0;
            double far = 1.0;
            double[] origins = {originX, originY, originZ};
            double[] deltas = {deltaX, deltaY, deltaZ};
            double[] minima = {minX, minY, minZ};
            double[] maxima = {maxX, maxY, maxZ};
            for (int axis = 0; axis < 3; axis++) {
                if (Math.abs(deltas[axis]) < 1.0E-12) {
                    if (origins[axis] < minima[axis] || origins[axis] > maxima[axis]) {
                        return -1.0;
                    }
                } else {
                    double first = (minima[axis] - origins[axis]) / deltas[axis];
                    double second = (maxima[axis] - origins[axis]) / deltas[axis];
                    near = Math.max(near, Math.min(first, second));
                    far = Math.min(far, Math.max(first, second));
                    if (near > far) {
                        return -1.0;
                    }
                }
            }
            return near;
        }

        private static boolean overlaps(double firstMin, double firstMax, double secondMin, double secondMax) {
            return firstMax > secondMin && firstMin < secondMax;
        }

        private static double clip(
                double movement,
                double movingMin,
                double movingMax,
                double obstacleMin,
                double obstacleMax
        ) {
            if (movement > 0.0 && movingMax <= obstacleMin) {
                return Math.min(movement, obstacleMin - movingMax);
            }
            if (movement < 0.0 && movingMin >= obstacleMax) {
                return Math.max(movement, obstacleMax - movingMin);
            }
            return movement;
        }
    }
}
