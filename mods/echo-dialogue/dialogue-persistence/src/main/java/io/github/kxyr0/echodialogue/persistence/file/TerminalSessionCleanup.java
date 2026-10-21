package io.github.kxyr0.echodialogue.persistence.file;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Bounded cleanup policy for retained terminal records.
 */
final class TerminalSessionCleanup {
    private final SessionFileStore store;

    TerminalSessionCleanup(SessionFileStore store) {
        this.store = store;
    }

    DialogueRepositoryWriteResult cleanup(
            List<DialogueRepositoryLoadResult> loaded,
            long nowEpochMillis,
            long maxAgeMillis,
            int maxRecords
    ) {
        List<DialogueCheckpoint> terminal = loaded.stream()
                .flatMap(result -> result.checkpoint().stream())
                .filter(this::terminal)
                .sorted(Comparator.comparingLong(DialogueCheckpoint::updatedAtEpochMillis)
                        .thenComparing(value -> value.sessionId().toString()))
                .toList();
        LinkedHashSet<DialogueSessionId> victims = victims(terminal, nowEpochMillis, maxAgeMillis, maxRecords);
        int deleted = deleteVictims(victims);
        return DialogueRepositoryWriteResult.completed(deleted, diagnostics(victims.size(), deleted));
    }

    private LinkedHashSet<DialogueSessionId> victims(
            List<DialogueCheckpoint> terminal,
            long nowEpochMillis,
            long maxAgeMillis,
            int maxRecords
    ) {
        LinkedHashSet<DialogueSessionId> victims = new LinkedHashSet<>();
        terminal.stream()
                .filter(value -> nowEpochMillis - value.updatedAtEpochMillis() > maxAgeMillis)
                .map(DialogueCheckpoint::sessionId)
                .forEach(victims::add);
        terminal.stream()
                .limit(Math.max(0, terminal.size() - maxRecords))
                .map(DialogueCheckpoint::sessionId)
                .forEach(victims::add);
        return victims;
    }

    private int deleteVictims(LinkedHashSet<DialogueSessionId> victims) {
        int deleted = 0;
        for (DialogueSessionId sessionId : victims) {
            if (deleteQuietly(sessionId)) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean deleteQuietly(DialogueSessionId sessionId) {
        try {
            store.delete(sessionId);
            return true;
        } catch (IOException exception) {
            return false;
        }
    }

    private List<DialogueDiagnostic> diagnostics(int requested, int deleted) {
        if (requested == 0) {
            return List.of();
        }
        List<DialogueDiagnostic> diagnostics = new ArrayList<>();
        diagnostics.add(diagnostic("EDLG-PERSIST-004", DiagnosticCategory.TERMINAL_CLEANUP_PERFORMED,
                "Выполнена очистка terminal session records: " + deleted));
        if (deleted < requested) {
            diagnostics.add(diagnostic("EDLG-PERSIST-012", DiagnosticCategory.SESSION_SAVE_FAILED,
                    "Часть terminal session records не удалось удалить: " + (requested - deleted)));
        }
        return diagnostics;
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
