package me.mss1r.axiomata.blueprint.tracing;

public final class OutlineMask {
    private final int resolution;
    private final byte[] bits;
    private final int cells;

    public OutlineMask(int resolution, byte[] bits) {
        if (resolution <= 0 || resolution % 8 != 0) {
            throw new IllegalArgumentException("Outline resolution must be a positive multiple of eight");
        }
        int expected = resolution * resolution / 8;
        if (bits.length != expected) {
            throw new IllegalArgumentException("Outline mask must hold " + expected + " bytes, not " + bits.length);
        }
        this.resolution = resolution;
        this.bits = bits;

        int counted = 0;
        for (byte value : bits) {
            counted += Integer.bitCount(value & 0xFF);
        }
        this.cells = counted;
    }

    public int resolution() {
        return resolution;
    }

    public int cells() {
        return cells;
    }

    public boolean contains(int x, int y) {
        return x >= 0 && y >= 0 && x < resolution && y < resolution;
    }

    public boolean ink(int x, int y) {
        if (!contains(x, y)) {
            return false;
        }
        int index = y * resolution + x;
        return (bits[index >> 3] & (0x80 >>> (index & 7))) != 0;
    }

    public byte[] bits() {
        return bits.clone();
    }
}
