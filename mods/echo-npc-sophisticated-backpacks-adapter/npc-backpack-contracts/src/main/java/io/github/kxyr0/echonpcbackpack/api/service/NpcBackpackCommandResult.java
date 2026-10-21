package io.github.kxyr0.echonpcbackpack.api.service;

import io.github.kxyr0.echonpcbackpack.api.identity.NpcBackpackCommandId;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackOutcome;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackView;
import java.util.Objects;
import java.util.Optional;

public record NpcBackpackCommandResult(NpcBackpackOutcome outcome, NpcBackpackCommandId commandId,
        Optional<NpcBackpackView> view, String diagnosticCode, String message) {
    public NpcBackpackCommandResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        commandId = Objects.requireNonNull(commandId, "commandId");
        view = Objects.requireNonNull(view, "view");
        diagnosticCode = Objects.requireNonNull(diagnosticCode, "diagnosticCode");
        message = Objects.requireNonNull(message, "message");
    }

    public boolean terminal() {
        return outcome != NpcBackpackOutcome.ACCEPTED;
    }
}
