package io.github.kxyr0.echonpcinteraction.forge.runtime;

import io.github.kxyr0.echonpcinteraction.api.result.NpcInteractionResult;
import java.util.Objects;
import java.util.Optional;

record StartOutcome(Optional<InteractionRecord> record, NpcInteractionResult result) {
    StartOutcome {
        record = Objects.requireNonNull(record, "record");
        result = Objects.requireNonNull(result, "result");
    }

    static StartOutcome accepted(InteractionRecord record, NpcInteractionResult result) {
        return new StartOutcome(Optional.of(record), result);
    }

    static StartOutcome failed(NpcInteractionResult result) {
        return new StartOutcome(Optional.empty(), result);
    }
}
