package me.mss1r.axiomata.collision;

import net.minecraft.core.Direction;

public record ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis, double pathTopY,
                             double approachAtPathTop, double approachPerRise,
                             double releaseY, double approachAtRelease, double climbFacing) {

    // Facing is authored with the ladder. Deriving it from the climber's position flips the pose
    // when the same ladder can be entered from both sides.
    public static final double FACING_POSITIVE = 1.0D;
    public static final double FACING_NEGATIVE = -1.0D;
    public ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis) {
        this(collision, approachAxis, Double.POSITIVE_INFINITY, Double.NaN, 0.0D,
                Double.POSITIVE_INFINITY, Double.NaN, FACING_POSITIVE);
    }

    public ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis, double topExitY) {
        this(collision, approachAxis, topExitY, FACING_POSITIVE);
    }

    public ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis, double topExitY,
                          double climbFacing) {
        this(collision, approachAxis, topExitY, Double.NaN, 0.0D, topExitY, Double.NaN, climbFacing);
    }

    public ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis, double pathTopY,
                          double approachAtPathTop, double approachPerRise) {
        this(collision, approachAxis, pathTopY, approachAtPathTop, approachPerRise,
                pathTopY, approachAtPathTop, FACING_POSITIVE);
    }

    public ClimbableGroup(CollisionGroup collision, Direction.Axis approachAxis, double pathTopY,
                          double approachAtPathTop, double approachPerRise,
                          double releaseY, double approachAtRelease) {
        this(collision, approachAxis, pathTopY, approachAtPathTop, approachPerRise,
                releaseY, approachAtRelease, FACING_POSITIVE);
    }

    public ClimbableGroup {
        if (approachAxis == Direction.Axis.Y) {
            throw new IllegalArgumentException("A climbable approach axis must be horizontal");
        }
        if (Double.isNaN(pathTopY) || Double.isNaN(releaseY) || releaseY < pathTopY) {
            throw new IllegalArgumentException("A climbable release must be at or above its path top");
        }
        if (!Double.isFinite(approachPerRise)) {
            throw new IllegalArgumentException("A climbable path slope must be finite");
        }
        if (approachPerRise != 0.0D && !Double.isFinite(approachAtPathTop)) {
            throw new IllegalArgumentException("A sloped climbable path needs a finite top coordinate");
        }
        if (releaseY > pathTopY
                && (!Double.isFinite(approachAtPathTop) || !Double.isFinite(approachAtRelease))) {
            throw new IllegalArgumentException("A climbable landing path needs finite coordinates");
        }
    }
}
