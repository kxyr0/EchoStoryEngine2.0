package io.github.kxyr0.echonpcinteraction.core.provider;

import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionProviderId;
import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionFailure;
import java.util.Objects;
import java.util.Optional;

public record NpcInteractionProviderChoice(
        Optional<NpcInteractionProviderId> providerId,
        Optional<NpcInteractionFailure> failure,
        String message) {
    public NpcInteractionProviderChoice {
        providerId = Objects.requireNonNull(providerId, "providerId");
        failure = Objects.requireNonNull(failure, "failure");
        message = Objects.requireNonNull(message, "message");
    }

    public boolean selected() {
        return providerId.isPresent() && failure.isEmpty();
    }
}
