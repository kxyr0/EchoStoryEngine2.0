package io.github.kxyr0.echonpcinteraction.api.result;

import io.github.kxyr0.echonpcinteraction.api.diagnostic.NpcInteractionDiagnostic;
import io.github.kxyr0.echonpcinteraction.api.identity.NpcInteractionCommandId;
import java.util.Objects;
import java.util.Optional;

public record NpcInteractionResult(
        NpcInteractionCommandId commandId,
        NpcInteractionStatus status,
        NpcInteractionOutcome outcome,
        Optional<NpcInteractionFailure> failure,
        Optional<NpcInteractionReceipt> receipt,
        Optional<NpcInteractionDiagnostic> diagnostic,
        String message) {
    public NpcInteractionResult {
        commandId = Objects.requireNonNull(commandId, "commandId");
        status = Objects.requireNonNull(status, "status");
        outcome = Objects.requireNonNull(outcome, "outcome");
        failure = Objects.requireNonNull(failure, "failure");
        receipt = Objects.requireNonNull(receipt, "receipt");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
        message = Objects.requireNonNull(message, "message");
    }

    public static NpcInteractionResult accepted(NpcInteractionCommandId commandId, String message) {
        return running(commandId, NpcInteractionStatus.CREATED, message);
    }

    public static NpcInteractionResult running(
            NpcInteractionCommandId commandId,
            NpcInteractionStatus status,
            String message) {
        return new NpcInteractionResult(commandId, status,
                NpcInteractionOutcome.COMPLETED, Optional.empty(), Optional.empty(), Optional.empty(), message);
    }

    public static NpcInteractionResult completed(NpcInteractionReceipt receipt, String message) {
        return new NpcInteractionResult(receipt.commandId(), NpcInteractionStatus.COMPLETED,
                NpcInteractionOutcome.COMPLETED, Optional.empty(), Optional.of(receipt), Optional.empty(), message);
    }

    public static NpcInteractionResult failed(NpcInteractionCommandId commandId, NpcInteractionOutcome outcome,
            NpcInteractionFailure failure, String message) {
        return new NpcInteractionResult(commandId, NpcInteractionStatus.FAILED, outcome,
                Optional.of(failure), Optional.empty(), Optional.empty(), message);
    }
}
