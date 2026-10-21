package io.github.kxyr0.echodialogue.api.observation;

/**
 * Closeable subscription returned by terminal observation registration.
 */
@FunctionalInterface
public interface DialogueObservationHandle extends AutoCloseable {
    @Override
    void close();
}
