package io.github.kxyr0.echoanimation.forge.client.debug;

/**
 * Screen-space face debug point for hover labels.
 */
public record FaceDebugPoint(String label, int x, int y, int color) {
    double distanceSq(int targetX, int targetY) {
        int dx = x - targetX;
        int dy = y - targetY;
        return dx * dx + dy * dy;
    }
}
