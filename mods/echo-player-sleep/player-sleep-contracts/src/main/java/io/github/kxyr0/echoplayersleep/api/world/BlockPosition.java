package io.github.kxyr0.echoplayersleep.api.world;

public record BlockPosition(int x, int y, int z) {
    public String compact() {
        return x + "," + y + "," + z;
    }
}
