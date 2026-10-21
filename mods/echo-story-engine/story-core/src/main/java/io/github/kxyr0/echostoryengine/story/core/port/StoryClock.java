package io.github.kxyr0.echostoryengine.story.core.port;

import java.time.Instant;

/**
 * Time source supplied by an adapter so domain transitions do not read global time.
 */
@FunctionalInterface
public interface StoryClock {
    Instant now();
}
