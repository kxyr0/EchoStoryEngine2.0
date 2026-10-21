package io.github.kxyr0.echodialogue.presentation.api;

/**
 * One server-scoped provider session.
 */
public interface SpeakerPresentationProviderSession extends AutoCloseable {
    SpeakerPresentationResolution resolve(SpeakerPresentationRequest request);

    @Override
    default void close() {
    }
}
