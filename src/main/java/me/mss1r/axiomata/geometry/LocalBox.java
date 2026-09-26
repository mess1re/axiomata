package me.mss1r.axiomata.geometry;

public record LocalBox(double minX, double minY, double minZ,
                       double maxX, double maxY, double maxZ) {
    public LocalBox {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Minimum coordinates must not exceed maximum coordinates");
        }
    }

    public boolean contains(double x, double y, double z) {
        return contains(x, y, z, 0.0D);
    }

    public boolean contains(double x, double y, double z, double margin) {
        double safeMargin = Math.max(0.0D, margin);
        return x >= minX - safeMargin && x <= maxX + safeMargin
                && y >= minY - safeMargin && y <= maxY + safeMargin
                && z >= minZ - safeMargin && z <= maxZ + safeMargin;
    }
}

