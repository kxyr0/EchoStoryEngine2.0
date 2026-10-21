package io.github.kxyr0.echoplayeractor.forge.movement;

import io.github.kxyr0.echoplayeractor.api.request.PlayerActorMoveRequest;
import java.util.Objects;

final class ActiveMove {
    private final PlayerActorMoveRequest request;
    private int elapsedTicks;
    private int stuckTicks;
    private double lastX;
    private double lastY;
    private double lastZ;

    ActiveMove(PlayerActorMoveRequest request, double x, double y, double z) {
        this.request = Objects.requireNonNull(request, "request");
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
    }

    PlayerActorMoveRequest request() {
        return request;
    }

    int advance() {
        elapsedTicks += 1;
        return elapsedTicks;
    }

    int updateStuck(double x, double y, double z) {
        double moved = Math.abs(x - lastX) + Math.abs(y - lastY) + Math.abs(z - lastZ);
        lastX = x;
        lastY = y;
        lastZ = z;
        stuckTicks = moved < 0.001D ? stuckTicks + 1 : 0;
        return stuckTicks;
    }
}
