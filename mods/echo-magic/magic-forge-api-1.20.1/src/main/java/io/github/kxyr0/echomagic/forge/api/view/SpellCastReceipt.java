package io.github.kxyr0.echomagic.forge.api.view;

import io.github.kxyr0.echomagic.api.cast.SpellCastOutcome;
import io.github.kxyr0.echomagic.api.cast.SpellCastStatus;
import io.github.kxyr0.echomagic.api.identity.SpellCastId;
import io.github.kxyr0.echomagic.api.identity.SpellId;
import java.util.Objects;
import java.util.Optional;

public record SpellCastReceipt(Optional<SpellCastId> castId, Optional<SpellId> spellId,
        SpellCastStatus status, Optional<SpellCastOutcome> outcome, long serverTick) {
    public SpellCastReceipt {
        castId = Objects.requireNonNull(castId, "castId");
        spellId = Objects.requireNonNull(spellId, "spellId");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
    }

    public boolean accepted() { return status == SpellCastStatus.ACCEPTED; }
}
