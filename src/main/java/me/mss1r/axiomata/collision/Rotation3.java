package me.mss1r.axiomata.collision;

import net.minecraft.world.phys.Vec3;

public record Rotation3(
        double m00, double m01, double m02,
        double m10, double m11, double m12,
        double m20, double m21, double m22
) {
    public static final Rotation3 IDENTITY = new Rotation3(1, 0, 0, 0, 1, 0, 0, 0, 1);

    public static Rotation3 aroundX(float radians) {
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Rotation3(1, 0, 0, 0, cos, -sin, 0, sin, cos);
    }

    public static Rotation3 aroundY(float radians) {
        double sin = Math.sin(radians);
        double cos = Math.cos(radians);
        return new Rotation3(cos, 0, sin, 0, 1, 0, -sin, 0, cos);
    }

    public Rotation3 multiply(Rotation3 other) {
        return new Rotation3(
                m00 * other.m00 + m01 * other.m10 + m02 * other.m20,
                m00 * other.m01 + m01 * other.m11 + m02 * other.m21,
                m00 * other.m02 + m01 * other.m12 + m02 * other.m22,
                m10 * other.m00 + m11 * other.m10 + m12 * other.m20,
                m10 * other.m01 + m11 * other.m11 + m12 * other.m21,
                m10 * other.m02 + m11 * other.m12 + m12 * other.m22,
                m20 * other.m00 + m21 * other.m10 + m22 * other.m20,
                m20 * other.m01 + m21 * other.m11 + m22 * other.m21,
                m20 * other.m02 + m21 * other.m12 + m22 * other.m22);
    }

    public Rotation3 transpose() {
        return new Rotation3(
                m00, m10, m20,
                m01, m11, m21,
                m02, m12, m22);
    }

    public Vec3 transform(Vec3 value) {
        return new Vec3(
                m00 * value.x + m01 * value.y + m02 * value.z,
                m10 * value.x + m11 * value.y + m12 * value.z,
                m20 * value.x + m21 * value.y + m22 * value.z);
    }

    public Vec3 transformInverse(Vec3 value) {
        return new Vec3(
                m00 * value.x + m10 * value.y + m20 * value.z,
                m01 * value.x + m11 * value.y + m21 * value.z,
                m02 * value.x + m12 * value.y + m22 * value.z);
    }

    public Vec3 axis(int index) {
        return switch (index) {
            case 0 -> new Vec3(m00, m10, m20);
            case 1 -> new Vec3(m01, m11, m21);
            case 2 -> new Vec3(m02, m12, m22);
            default -> throw new IllegalArgumentException("rotation axis index must be 0, 1 or 2");
        };
    }

    public double element(int row, int column) {
        return switch (row * 3 + column) {
            case 0 -> m00;
            case 1 -> m01;
            case 2 -> m02;
            case 3 -> m10;
            case 4 -> m11;
            case 5 -> m12;
            case 6 -> m20;
            case 7 -> m21;
            default -> m22;
        };
    }
}
