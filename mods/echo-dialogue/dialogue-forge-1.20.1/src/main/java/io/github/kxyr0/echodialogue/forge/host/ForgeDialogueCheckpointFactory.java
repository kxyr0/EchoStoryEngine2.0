package io.github.kxyr0.echodialogue.forge.host;

import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueTransition;
import io.github.kxyr0.echodialogue.core.session.DialogueTransitionStatus;
import java.util.function.LongSupplier;

/**
 * Maps server transitions to portable durable checkpoints.
 */
final class ForgeDialogueCheckpointFactory {
    private final LongSupplier clock;

    ForgeDialogueCheckpointFactory(LongSupplier clock) {
        this.clock = clock;
    }

    DialogueCheckpoint checkpoint(DialogueTransition transition, CompiledDialogue dialogue) {
        var fingerprint = CompiledDialogueFingerprinter.fingerprint(dialogue);
        if (transition.status() == DialogueTransitionStatus.COMPLETED) {
            return DialogueCheckpoint.terminal(transition.state(), dialogue, fingerprint, clock.getAsLong(),
                    DialogueCheckpoint.TerminalFields.completed(transition.state().outcomeId().orElseThrow()));
        }
        if (transition.status() == DialogueTransitionStatus.CANCELLED) {
            return DialogueCheckpoint.terminal(transition.state(), dialogue, fingerprint, clock.getAsLong(),
                    DialogueCheckpoint.TerminalFields.cancelled("explicit-cancel"));
        }
        if (transition.status() == DialogueTransitionStatus.FAILED) {
            return DialogueCheckpoint.terminal(transition.state(), dialogue, fingerprint, clock.getAsLong(),
                    DialogueCheckpoint.TerminalFields.empty());
        }
        return DialogueCheckpoint.active(transition.state(), dialogue, fingerprint, clock.getAsLong());
    }
}
