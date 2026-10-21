package io.github.kxyr0.echobackgroundnpc.core.render;

import io.github.kxyr0.echobackgroundnpc.core.navigation.WorldPoint;
import java.util.Objects;

/** Allocation-free two-snapshot interpolation state for one visible agent. */
public final class ClientMovementInterpolator {
    private MovementSnapshot previous;
    private MovementSnapshot current;

    public boolean accept(MovementSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (current != null && snapshot.revision() <= current.revision()) return false;
        previous = current;
        current = snapshot;
        return true;
    }

    public WorldPoint sample(double renderServerTick) {
        if (current == null) throw new IllegalStateException("No movement snapshot.");
        if (previous == null || current.serverTick() == previous.serverTick()) return current.position();
        double alpha = (renderServerTick - previous.serverTick())
                / (double) (current.serverTick() - previous.serverTick());
        return previous.position().lerp(current.position(), alpha);
    }
}
