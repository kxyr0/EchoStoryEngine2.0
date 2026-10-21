package io.github.kxyr0.echoik.runtime;

import java.util.Optional;

/**
 * Provides already-resolved interaction target descriptions.
 */
public interface InteractionTargetProvider {
    Optional<InteractionTarget> target(String subjectId, String interactionId);
}
