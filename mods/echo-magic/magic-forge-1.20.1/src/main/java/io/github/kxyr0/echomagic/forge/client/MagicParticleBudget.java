package io.github.kxyr0.echomagic.forge.client;

import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import java.util.ArrayDeque;
import java.util.Queue;

public final class MagicParticleBudget {
    private final Queue<Long> expirations = new ArrayDeque<>();
    private int frameCount;

    public void beginFrame() { frameCount = 0; }

    public boolean acquire(long gameTick, int lifetime, boolean core) {
        while (!expirations.isEmpty() && expirations.peek() <= gameTick) expirations.remove();
        if (!core && (frameCount >= MagicClientConfig.MAXIMUM_PARTICLES_PER_FRAME.get()
                || expirations.size() >= MagicClientConfig.MAXIMUM_PARTICLES_ACTIVE.get())) return false;
        frameCount++;
        expirations.add(gameTick + lifetime);
        return true;
    }

    public int active() { return expirations.size(); }
}

