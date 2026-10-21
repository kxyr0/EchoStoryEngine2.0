package io.github.kxyr0.echonpc.api.spatial;

/**
 * Immutable neutral integer block position. Forge converts it to Minecraft BlockPos.
 */
public record BlockPosition(int x, int y, int z) {
    private static final int MAX_HORIZONTAL = 30_000_000;
    private static final int MIN_Y = -2048;
    private static final int MAX_Y = 4096;

    public BlockPosition {
        if (Math.abs(x) > MAX_HORIZONTAL || Math.abs(z) > MAX_HORIZONTAL || y < MIN_Y || y > MAX_Y) {
            throw new IllegalArgumentException("BlockPosition is outside Echo NPC bounds.");
        }
    }

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }
}
