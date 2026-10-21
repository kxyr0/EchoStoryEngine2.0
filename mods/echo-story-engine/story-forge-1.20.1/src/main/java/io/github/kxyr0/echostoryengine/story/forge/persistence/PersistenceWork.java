package io.github.kxyr0.echostoryengine.story.forge.persistence;

/**
 * Background persistence work that returns server-thread completion.
 */
@FunctionalInterface
public interface PersistenceWork {
    Runnable run();
}
