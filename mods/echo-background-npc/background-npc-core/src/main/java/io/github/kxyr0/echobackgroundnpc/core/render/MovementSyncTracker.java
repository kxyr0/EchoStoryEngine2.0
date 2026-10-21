package io.github.kxyr0.echobackgroundnpc.core.render;

import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import java.util.HashMap;
import java.util.Map;

/** Change-based movement sync with a bounded periodic resync. */
public final class MovementSyncTracker {
    private final Map<BackgroundNpcId, MovementSnapshot> sent = new HashMap<>();
    private final long resyncTicks;
    private final double positionEpsilonSquared;

    public MovementSyncTracker(long resyncTicks, double positionEpsilon) {
        if (resyncTicks < 1L || positionEpsilon < 0.0) throw new IllegalArgumentException("Invalid sync policy.");
        this.resyncTicks = resyncTicks;
        positionEpsilonSquared = positionEpsilon * positionEpsilon;
    }

    public boolean shouldSend(MovementSnapshot candidate) {
        MovementSnapshot previous = sent.get(candidate.agentId());
        boolean changed = previous == null || previous.task() != candidate.task()
                || previous.position().distanceSquared(candidate.position()) > positionEpsilonSquared
                || Math.abs(previous.facing() - candidate.facing()) > 1.0f;
        boolean resync = previous == null || candidate.serverTick() - previous.serverTick() >= resyncTicks;
        if (!changed && !resync) return false;
        sent.put(candidate.agentId(), candidate);
        return true;
    }

    public void remove(BackgroundNpcId id) { sent.remove(id); }
    public void clear() { sent.clear(); }
}
