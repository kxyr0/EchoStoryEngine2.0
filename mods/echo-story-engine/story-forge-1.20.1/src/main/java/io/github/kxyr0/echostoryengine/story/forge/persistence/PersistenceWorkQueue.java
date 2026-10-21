package io.github.kxyr0.echostoryengine.story.forge.persistence;

import java.time.Duration;

/**
 * Bounded queue для файловых операций, которые не вызывают Minecraft APIs.
 */
public interface PersistenceWorkQueue {
    boolean submit(PersistenceWork work);

    int drain(int maxResults);

    void shutdown(Duration timeout);

    int pendingCount();

    int capacity();
}
