package io.github.kxyr0.echodialogue.persistence.recovery;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprinter;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogue;
import io.github.kxyr0.echodialogue.core.model.CompiledDialogueNode;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import java.util.Map;
import java.util.Optional;

/**
 * Resolves durable checkpoints against the current compiled catalog.
 */
public final class DialogueRecoveryPlanner {
    public DialogueRecoveryPlan plan(DialogueCheckpoint checkpoint, Map<DialogueId, CompiledDialogue> catalog) {
        if (terminal(checkpoint)) {
            return DialogueRecoveryPlan.restored(DialogueRecoveryPlan.Kind.TERMINAL, checkpoint);
        }
        if (checkpoint.phase() != DialogueSessionPhase.ACTIVE
                && checkpoint.phase() != DialogueSessionPhase.WAITING) {
            return DialogueRecoveryPlan.corrupt(diagnostic(
                    "EDLG-PERSIST-020", DiagnosticCategory.SNAPSHOT_MALFORMED,
                    "Snapshot содержит неподдерживаемую phase для recovery."));
        }
        CompiledDialogue dialogue = catalog.get(checkpoint.dialogueId());
        if (dialogue == null) {
            return DialogueRecoveryPlan.blocked(diagnostic(
                    "EDLG-PERSIST-021", DiagnosticCategory.COMPILED_DIALOGUE_MISSING,
                    "Compiled dialogue для snapshot не найден."), checkpoint);
        }
        Optional<io.github.kxyr0.echodialogue.api.identity.DialogueNodeId> resolved =
                dialogue.resolveNodeId(checkpoint.currentNodeId());
        boolean migrated = resolved.isPresent() && !resolved.orElseThrow().equals(checkpoint.currentNodeId());
        DialogueCheckpoint effective = migrated
                ? checkpoint.withCurrentNodeId(resolved.orElseThrow()) : checkpoint;
        boolean fingerprintMatches = CompiledDialogueFingerprinter.fingerprint(dialogue)
                .equals(checkpoint.compiledFingerprint());
        if (!migrated && !fingerprintMatches) {
            return DialogueRecoveryPlan.blocked(diagnostic(
                    "EDLG-PERSIST-022", DiagnosticCategory.FINGERPRINT_INCOMPATIBLE,
                    "Compiled dialogue fingerprint не совпадает со snapshot."), checkpoint);
        }
        return cursorValid(effective, dialogue)
                ? DialogueRecoveryPlan.restored(DialogueRecoveryPlan.Kind.ACTIVE_DETACHED, effective)
                : DialogueRecoveryPlan.corrupt(diagnostic(
                        "EDLG-PERSIST-023", DiagnosticCategory.SNAPSHOT_MALFORMED,
                        "Snapshot содержит недопустимый NodeId или line cursor; автоматический fallback запрещён."));
    }

    private boolean cursorValid(DialogueCheckpoint checkpoint, CompiledDialogue dialogue) {
        return dialogue.findNode(checkpoint.currentNodeId()).map(node -> cursorValid(checkpoint, node)).orElse(false);
    }

    private boolean cursorValid(DialogueCheckpoint checkpoint, CompiledDialogueNode node) {
        if (checkpoint.presentationKind() == DialoguePresentationKind.CHOICE) {
            return !node.choices().isEmpty();
        }
        return checkpoint.lineOrdinal() >= 0 && checkpoint.lineOrdinal() < node.lines().size();
    }

    private boolean terminal(DialogueCheckpoint checkpoint) {
        return checkpoint.phase() == DialogueSessionPhase.COMPLETED
                || checkpoint.phase() == DialogueSessionPhase.CANCELLED
                || checkpoint.phase() == DialogueSessionPhase.FAILED;
    }

    private DialogueDiagnostic diagnostic(String code, DiagnosticCategory category, String message) {
        return DialogueDiagnostic.error(code, category, message);
    }
}
