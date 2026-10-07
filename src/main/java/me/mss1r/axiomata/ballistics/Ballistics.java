package me.mss1r.axiomata.ballistics;

import net.minecraft.world.phys.Vec3;

/** Firing solutions using the same integration order as projectile flight. */
public final class Ballistics {
    /** Gravity, 9.81 m/s², in blocks/tick² (1 block = 1 m). */
    public static final double GRAVITY = 9.81D / 400.0D;
    private static final int MAX_FLIGHT_TICKS = 600;
    private static final double LOWEST_ELEVATION = -60.0D;
    private static final double HIGHEST_ELEVATION = 60.0D;
    private static final double ELEVATION_SCAN_STEP = 2.0D;
    private static final int SEARCH_STEPS = 32;

    private Ballistics() {
    }

    /**
     * Matches the projectile tick order: motor thrust, movement, quadratic air drag, then gravity.
     * Velocity is in blocks/tick; gravity and thrust are in blocks/tick².
     */
    public record Flight(double gravity, double airDrag, double thrust, int thrustTicks) {
        /** Ballistic flight under gravity only. */
        public static Flight ballistic(double airDrag) {
            return new Flight(GRAVITY, airDrag, 0.0D, 0);
        }

        /** Velocity after one tick of flight. */
        public Vec3 afterMove(Vec3 velocity) {
            double kept = Math.max(0.0D, 1.0D - airDrag * velocity.length());
            return new Vec3(velocity.x * kept, velocity.y * kept - gravity, velocity.z * kept);
        }
    }

    /** Returns Minecraft pitch for the lower firing solution, or {@link Float#NaN} if it cannot reach. */
    public static float calculateLowAnglePitch(Vec3 origin, Vec3 target, double speed, double gravity) {
        return calculateLowAnglePitch(origin, target, speed, new Flight(gravity, 0.0D, 0.0D, 0));
    }

    /** Returns Minecraft pitch for the lower firing solution, or {@link Float#NaN} if it cannot reach. */
    public static float calculateLowAnglePitch(Vec3 origin, Vec3 target, double speed, Flight flight) {
        double horizontalDistance = horizontalDistance(origin, target);
        double targetHeight = target.y - origin.y;
        if (horizontalDistance < 1.0E-4D) {
            return (float) -Math.toDegrees(Math.atan2(targetHeight, horizontalDistance));
        }
        if (speed <= 0.0D) {
            return Float.NaN;
        }

        double previous = LOWEST_ELEVATION;
        double previousError = elevationHeightError(horizontalDistance, targetHeight, speed, previous, flight);
        if (previousError >= 0.0D) {
            return (float) -previous;
        }
        for (double elevation = LOWEST_ELEVATION + ELEVATION_SCAN_STEP; elevation <= HIGHEST_ELEVATION;
             elevation += ELEVATION_SCAN_STEP) {
            double error = elevationHeightError(horizontalDistance, targetHeight, speed, elevation, flight);
            if (error >= 0.0D) {
                double low = previous;
                double high = elevation;
                for (int i = 0; i < SEARCH_STEPS; i++) {
                    double middle = (low + high) * 0.5D;
                    if (elevationHeightError(horizontalDistance, targetHeight, speed, middle, flight) >= 0.0D) {
                        high = middle;
                    } else {
                        low = middle;
                    }
                }
                return (float) -((low + high) * 0.5D);
            }
            previous = elevation;
        }
        return Float.NaN;
    }

    /** Returns the power multiplier for a fixed launch slope, without drag, or NaN if unreachable. */
    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity) {
        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        double height = target.y - origin.y;
        double angle = Math.atan(launchSlope);
        double denominator = 2.0D * Math.cos(angle) * Math.cos(angle)
                * (horizontalDistance * Math.tan(angle) - height);
        if (baseSpeed <= 0.0D || gravity <= 0.0D || denominator <= 0.0D) {
            return Double.NaN;
        }

        double requiredSpeedSquared = gravity * horizontalDistance * horizontalDistance / denominator;
        return requiredSpeedSquared <= 0.0D ? Double.NaN : Math.sqrt(requiredSpeedSquared) / baseSpeed;
    }

    /** Returns a bounded power multiplier with air drag, or NaN if the range has no solution. */
    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, double gravity, double airDrag,
                                                double minPower, double maxPower) {
        return calculateFixedArcPower(origin, target, baseSpeed, launchSlope,
                new Flight(gravity, airDrag, 0.0D, 0), minPower, maxPower);
    }

    /**
     * Horizontal distance until a shot launched at {@code speed} blocks/tick and {@code elevationDegrees} is {@code
     * drop} blocks below its launch height, or {@code 0} if it never gets there.
     */
    public static double range(double speed, double elevationDegrees, double drop, Flight flight) {
        double elevation = Math.toRadians(elevationDegrees);
        double horizontalVelocity = Math.cos(elevation) * speed;
        double verticalVelocity = Math.sin(elevation) * speed;
        double horizontalPosition = 0.0D;
        double verticalPosition = 0.0D;
        for (int tick = 0; tick < MAX_FLIGHT_TICKS; tick++) {
            if (tick < flight.thrustTicks()) {
                double[] pushed = thrust(horizontalVelocity, verticalVelocity, elevation, flight.thrust());
                horizontalVelocity = pushed[0];
                verticalVelocity = pushed[1];
            }
            double previousHorizontal = horizontalPosition;
            double previousVertical = verticalPosition;
            horizontalPosition += horizontalVelocity;
            verticalPosition += verticalVelocity;
            if (verticalPosition <= -drop) {
                double segment = previousVertical - verticalPosition;
                double progress = segment <= 1.0E-8D ? 1.0D : (previousVertical + drop) / segment;
                return previousHorizontal + (horizontalPosition - previousHorizontal) * progress;
            }
            Vec3 next = flight.afterMove(new Vec3(horizontalVelocity, verticalVelocity, 0.0D));
            horizontalVelocity = next.x;
            verticalVelocity = next.y;
        }
        return 0.0D;
    }

    /** Motor thrust direction: along the velocity, or along the launch line while at rest. */
    private static double[] thrust(double horizontalVelocity, double verticalVelocity, double elevation,
                                   double thrust) {
        double speed = Math.sqrt(horizontalVelocity * horizontalVelocity + verticalVelocity * verticalVelocity);
        double horizontal = speed > 1.0E-9D ? horizontalVelocity / speed : Math.cos(elevation);
        double vertical = speed > 1.0E-9D ? verticalVelocity / speed : Math.sin(elevation);
        return new double[]{horizontalVelocity + horizontal * thrust, verticalVelocity + vertical * thrust};
    }

    /** Returns a bounded power multiplier for a fixed launch slope, or NaN if the range has no solution. */
    public static double calculateFixedArcPower(Vec3 origin, Vec3 target, double baseSpeed,
                                                double launchSlope, Flight flight,
                                                double minPower, double maxPower) {
        if (baseSpeed <= 0.0D || flight.gravity() <= 0.0D || minPower <= 0.0D || maxPower < minPower) {
            return Double.NaN;
        }

        double horizontalDistance = horizontalDistance(origin, target);
        double targetHeight = target.y - origin.y;
        if (horizontalDistance < 1.0E-4D) {
            return Double.NaN;
        }

        double elevation = Math.toDegrees(Math.atan(launchSlope));
        double lowError = elevationHeightError(horizontalDistance, targetHeight, baseSpeed * minPower,
                elevation, flight);
        double highError = elevationHeightError(horizontalDistance, targetHeight, baseSpeed * maxPower,
                elevation, flight);
        if (!Double.isFinite(highError) || lowError > 0.0D || highError < 0.0D) {
            return Double.NaN;
        }

        double low = minPower;
        double high = maxPower;
        for (int i = 0; i < SEARCH_STEPS; i++) {
            double middle = (low + high) * 0.5D;
            double error = elevationHeightError(horizontalDistance, targetHeight, baseSpeed * middle,
                    elevation, flight);
            if (!Double.isFinite(error) || error < 0.0D) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return (low + high) * 0.5D;
    }

    private static double horizontalDistance(Vec3 origin, Vec3 target) {
        double dx = target.x - origin.x;
        double dz = target.z - origin.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    /** Height above the target when the shot reaches the target's distance, or -infinity if it falls short. */
    private static double elevationHeightError(double horizontalDistance, double targetHeight, double speed,
                                               double elevationDegrees, Flight flight) {
        double elevation = Math.toRadians(elevationDegrees);
        double directionHorizontal = Math.cos(elevation);
        double directionVertical = Math.sin(elevation);
        double horizontalVelocity = directionHorizontal * speed;
        double verticalVelocity = directionVertical * speed;
        double horizontalPosition = 0.0D;
        double verticalPosition = 0.0D;

        for (int tick = 0; tick < MAX_FLIGHT_TICKS; tick++) {
            if (tick < flight.thrustTicks()) {
                double[] pushed = thrust(horizontalVelocity, verticalVelocity, elevation, flight.thrust());
                horizontalVelocity = pushed[0];
                verticalVelocity = pushed[1];
            }
            double previousHorizontal = horizontalPosition;
            double previousVertical = verticalPosition;
            horizontalPosition += horizontalVelocity;
            verticalPosition += verticalVelocity;
            if (horizontalPosition >= horizontalDistance) {
                double segment = horizontalPosition - previousHorizontal;
                double progress = segment <= 1.0E-8D
                        ? 1.0D
                        : (horizontalDistance - previousHorizontal) / segment;
                return previousVertical + (verticalPosition - previousVertical) * progress - targetHeight;
            }

            Vec3 next = flight.afterMove(new Vec3(horizontalVelocity, verticalVelocity, 0.0D));
            horizontalVelocity = next.x;
            verticalVelocity = next.y;
            if (horizontalVelocity <= 1.0E-6D) {
                break;
            }
        }
        return Double.NEGATIVE_INFINITY;
    }
}
