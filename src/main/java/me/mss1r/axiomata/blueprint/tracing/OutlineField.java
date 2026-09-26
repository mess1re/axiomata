package me.mss1r.axiomata.blueprint.tracing;

public final class OutlineField {
    private static final float INFINITY = Float.MAX_VALUE / 4.0F;

    private final int resolution;
    private final int[] nearest;
    private final int[] distanceSquared;

    public OutlineField(OutlineMask mask) {
        this.resolution = mask.resolution();
        this.nearest = new int[resolution * resolution];
        this.distanceSquared = new int[resolution * resolution];

        int[] columnFeature = new int[resolution * resolution];
        float[] columnDistance = new float[resolution * resolution];
        float[] source = new float[resolution];
        float[] result = new float[resolution];
        int[] winner = new int[resolution];
        int[] hull = new int[resolution];
        float[] boundary = new float[resolution + 1];

        for (int x = 0; x < resolution; x++) {
            for (int y = 0; y < resolution; y++) {
                source[y] = mask.ink(x, y) ? 0.0F : INFINITY;
            }
            transform(source, result, winner, resolution, hull, boundary);
            for (int y = 0; y < resolution; y++) {
                columnDistance[y * resolution + x] = result[y];
                columnFeature[y * resolution + x] = winner[y];
            }
        }

        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {
                source[x] = columnDistance[y * resolution + x];
            }
            transform(source, result, winner, resolution, hull, boundary);
            for (int x = 0; x < resolution; x++) {
                int featureX = winner[x];
                int featureY = columnFeature[y * resolution + featureX];
                int index = y * resolution + x;
                nearest[index] = featureY * resolution + featureX;
                distanceSquared[index] = result[x] >= INFINITY ? Integer.MAX_VALUE : Math.round(result[x]);
            }
        }
    }

    private static void transform(float[] source, float[] result, int[] winner, int length,
                                  int[] hull, float[] boundary) {
        int rightmost = 0;
        hull[0] = 0;
        boundary[0] = -INFINITY;
        boundary[1] = INFINITY;

        for (int position = 1; position < length; position++) {
            float intersection;
            while (true) {
                int previous = hull[rightmost];
                intersection = ((source[position] + (float) position * position)
                        - (source[previous] + (float) previous * previous))
                        / (2.0F * position - 2.0F * previous);
                if (intersection > boundary[rightmost]) {
                    break;
                }
                rightmost--;
            }
            rightmost++;
            hull[rightmost] = position;
            boundary[rightmost] = intersection;
            boundary[rightmost + 1] = INFINITY;
        }

        rightmost = 0;
        for (int position = 0; position < length; position++) {
            while (boundary[rightmost + 1] < position) {
                rightmost++;
            }
            int source_ = hull[rightmost];
            int offset = position - source_;
            result[position] = (float) offset * offset + source[source_];
            winner[position] = source_;
        }
    }

    public int resolution() {
        return resolution;
    }

    public int distanceSquared(int x, int y) {
        return distanceSquared[y * resolution + x];
    }

    public int nearestX(int x, int y) {
        return nearest[y * resolution + x] % resolution;
    }

    public int nearestY(int x, int y) {
        return nearest[y * resolution + x] / resolution;
    }
}
