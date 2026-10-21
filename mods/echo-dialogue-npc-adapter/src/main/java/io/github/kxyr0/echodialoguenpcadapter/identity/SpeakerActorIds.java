package io.github.kxyr0.echodialoguenpcadapter.identity;

import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import java.util.Objects;
import java.util.Optional;

public final class SpeakerActorIds {
    private SpeakerActorIds() {
    }

    public static Optional<ActorId> actorId(SpeakerId speakerId) {
        try {
            return Optional.of(new ActorId(Objects.requireNonNull(speakerId, "speakerId").toString()));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
