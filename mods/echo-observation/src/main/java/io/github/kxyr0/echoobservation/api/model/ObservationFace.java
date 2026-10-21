package io.github.kxyr0.echoobservation.api.model;

public enum ObservationFace {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int x;
    private final int y;
    private final int z;

    ObservationFace(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int normalX() { return x; }
    public int normalY() { return y; }
    public int normalZ() { return z; }
}
