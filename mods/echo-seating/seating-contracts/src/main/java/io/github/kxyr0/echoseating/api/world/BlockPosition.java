package io.github.kxyr0.echoseating.api.world;

public record BlockPosition(int x, int y, int z) {
    public static BlockPosition containing(Vector3 position) {
        return new BlockPosition(floor(position.x()), floor(position.y()), floor(position.z()));
    }

    private static int floor(double value) {
        int truncated = (int) value;
        return value < truncated ? truncated - 1 : truncated;
    }
}
