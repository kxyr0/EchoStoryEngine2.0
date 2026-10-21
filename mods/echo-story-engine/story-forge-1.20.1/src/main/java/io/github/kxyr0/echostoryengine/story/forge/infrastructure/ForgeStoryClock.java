package io.github.kxyr0.echostoryengine.story.forge.infrastructure;

import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import java.time.Instant;

/**
 * Platform time source for Forge adapters.
 */
public final class ForgeStoryClock implements StoryClock {
    @Override
    public Instant now() {
        return Instant.now();
    }
}
