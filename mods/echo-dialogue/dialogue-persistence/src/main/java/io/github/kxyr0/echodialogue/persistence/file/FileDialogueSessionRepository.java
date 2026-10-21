package io.github.kxyr0.echodialogue.persistence.file;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryWriteResult;
import io.github.kxyr0.echodialogue.core.persistence.DialogueSessionRepository;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.persistence.codec.DialogueSaveJsonCodec;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Portable synchronous file repository for durable dialogue sessions.
 */
public final class FileDialogueSessionRepository implements DialogueSessionRepository {
    private final SessionFileStore store;
    private final DialogueSaveJsonCodec codec;
    private final SessionSnapshotReader reader;

    public FileDialogueSessionRepository(Path root) {
        this(root, new DialogueSaveJsonCodec());
    }

    FileDialogueSessionRepository(Path root, DialogueSaveJsonCodec codec) {
        this.store = new SessionFileStore(root);
        this.codec = codec;
        this.reader = new SessionSnapshotReader(store, codec);
    }

    @Override
    public DialogueRepositoryLoadResult load(DialogueSessionId sessionId) {
        return reader.load(sessionId);
    }

    @Override
    public List<DialogueRepositoryLoadResult> loadAll() {
        return reader.loadAll();
    }

    @Override
    public DialogueRepositoryWriteResult save(DialogueCheckpoint checkpoint, SaveExpectation expectation) {
        DialogueRepositoryWriteResult conflict = conflictIfNeeded(checkpoint.sessionId(), expectation);
        if (conflict != null) {
            return conflict;
        }
        DialogueCheckpoint saved = nextRepositoryRevision(checkpoint, expectation);
        try {
            store.write(saved.sessionId(), codec.encode(saved));
            return DialogueRepositoryWriteResult.saved(saveKind(expectation), saved);
        } catch (IOException | RuntimeException exception) {
            return failed("EDLG-PERSIST-002", "Не удалось durably сохранить snapshot сессии.");
        }
    }

    @Override
    public DialogueRepositoryWriteResult acknowledgeTerminal(
            DialogueSessionId sessionId,
            DialogueRepositoryRevision expectedRevision
    ) {
        DialogueRepositoryLoadResult loaded = load(sessionId);
        if (loaded.kind() == DialogueRepositoryLoadResult.Kind.NOT_FOUND) {
            return DialogueRepositoryWriteResult.notFound();
        }
        if (loaded.kind() == DialogueRepositoryLoadResult.Kind.FAILED) {
            return DialogueRepositoryWriteResult.failed(loaded.diagnostics());
        }
        DialogueCheckpoint checkpoint = loaded.checkpoint().orElseThrow();
        if (!checkpoint.repositoryRevision().equals(expectedRevision)) {
            return conflict("EDLG-PERSIST-003", "Terminal acknowledgement получил stale repository revision.");
        }
        return deleteTerminal(checkpoint);
    }

    @Override
    public DialogueRepositoryWriteResult cleanupTerminal(long nowEpochMillis, long maxAgeMillis, int maxRecords) {
        return new TerminalSessionCleanup(store).cleanup(loadAll(), nowEpochMillis, maxAgeMillis, maxRecords);
    }

    private DialogueRepositoryWriteResult conflictIfNeeded(
            DialogueSessionId sessionId,
            SaveExpectation expectation
    ) {
        if (expectation.revision().isEmpty()) {
            return store.exists(sessionId) ? conflict("EDLG-PERSIST-007", "Snapshot уже существует.") : null;
        }
        DialogueRepositoryLoadResult loaded = load(sessionId);
        if (loaded.kind() != DialogueRepositoryLoadResult.Kind.FOUND) {
            return loaded.kind() == DialogueRepositoryLoadResult.Kind.NOT_FOUND
                    ? conflict("EDLG-PERSIST-008", "Snapshot для update не найден.")
                    : DialogueRepositoryWriteResult.failed(loaded.diagnostics());
        }
        DialogueRepositoryRevision expected = expectation.revision().orElseThrow();
        return loaded.checkpoint().orElseThrow().repositoryRevision().equals(expected)
                ? null
                : conflict("EDLG-PERSIST-009", "Repository revision не совпадает.");
    }

    private DialogueCheckpoint nextRepositoryRevision(
            DialogueCheckpoint checkpoint,
            SaveExpectation expectation
    ) {
        DialogueRepositoryRevision next = expectation.revision()
                .map(DialogueRepositoryRevision::next)
                .orElse(new DialogueRepositoryRevision(0));
        return checkpoint.withRepositoryRevision(next);
    }

    private DialogueRepositoryWriteResult.Kind saveKind(SaveExpectation expectation) {
        return expectation.revision().isEmpty()
                ? DialogueRepositoryWriteResult.Kind.CREATED
                : DialogueRepositoryWriteResult.Kind.UPDATED;
    }

    private DialogueRepositoryWriteResult deleteTerminal(DialogueCheckpoint checkpoint) {
        if (!terminal(checkpoint)) {
            return failed("EDLG-PERSIST-010", "ACTIVE snapshot нельзя удалить через terminal acknowledgement.");
        }
        try {
            store.delete(checkpoint.sessionId());
            return DialogueRepositoryWriteResult.completed(1, List.of());
        } catch (IOException exception) {
            return failed("EDLG-PERSIST-011", "Terminal snapshot не удалось удалить.");
        }
    }

    private boolean terminal(DialogueCheckpoint checkpoint) {
        return checkpoint.phase() == DialogueSessionPhase.COMPLETED
                || checkpoint.phase() == DialogueSessionPhase.CANCELLED
                || checkpoint.phase() == DialogueSessionPhase.FAILED;
    }

    private DialogueRepositoryWriteResult conflict(String code, String message) {
        return DialogueRepositoryWriteResult.conflict(List.of(diagnostic(
                code, DiagnosticCategory.REPOSITORY_REVISION_CONFLICT, message)));
    }

    private DialogueRepositoryWriteResult failed(String code, String message) {
        return DialogueRepositoryWriteResult.failed(List.of(diagnostic(
                code, DiagnosticCategory.SESSION_SAVE_FAILED, message)));
    }

    private DialogueDiagnostic diagnostic(String code, DiagnosticCategory category, String message) {
        return DialogueDiagnostic.error(code, category, message);
    }
}
