package io.github.kxyr0.echodialogue.persistence.file;

import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryLoadResult;
import io.github.kxyr0.echodialogue.persistence.codec.DialogueSaveJsonCodec;
import io.github.kxyr0.echodialogue.persistence.codec.DialogueSaveJsonCodec.SaveCodecException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Reads primary/backup snapshot pairs without mutating repository state.
 */
final class SessionSnapshotReader {
    private final SessionFileStore store;
    private final DialogueSaveJsonCodec codec;

    SessionSnapshotReader(SessionFileStore store, DialogueSaveJsonCodec codec) {
        this.store = store;
        this.codec = codec;
    }

    DialogueRepositoryLoadResult load(DialogueSessionId sessionId) {
        Path primary = store.primary(sessionId);
        return loadPair(primary, store.backupForPrimary(primary));
    }

    List<DialogueRepositoryLoadResult> loadAll() {
        try {
            return store.primaries().stream()
                    .map(primary -> loadPair(primary, store.backupForPrimary(primary)))
                    .toList();
        } catch (IOException exception) {
            return List.of(DialogueRepositoryLoadResult.failed(List.of(diagnostic(
                    "EDLG-PERSIST-001", DiagnosticCategory.SNAPSHOT_MALFORMED,
                    "Не удалось прочитать каталог snapshot-файлов."))));
        }
    }

    private DialogueRepositoryLoadResult loadPair(Path primary, Path backup) {
        LoadAttempt primaryAttempt = read(primary);
        if (primaryAttempt.checkpoint() != null) {
            return DialogueRepositoryLoadResult.found(primaryAttempt.checkpoint(), List.of());
        }
        LoadAttempt backupAttempt = read(backup);
        if (backupAttempt.checkpoint() != null) {
            return DialogueRepositoryLoadResult.found(backupAttempt.checkpoint(), List.of(diagnostic(
                    "EDLG-PERSIST-005", DiagnosticCategory.BACKUP_FALLBACK_USED,
                    "Primary snapshot повреждён; использован backup.")));
        }
        return loadFailure(primaryAttempt, backupAttempt);
    }

    private LoadAttempt read(Path path) {
        if (!Files.exists(path)) {
            return LoadAttempt.missingAttempt();
        }
        try {
            return LoadAttempt.found(codec.decode(store.read(path)));
        } catch (SaveCodecException exception) {
            return LoadAttempt.failed(exception.category());
        } catch (IOException | RuntimeException exception) {
            return LoadAttempt.failed(DiagnosticCategory.SNAPSHOT_MALFORMED);
        }
    }

    private DialogueRepositoryLoadResult loadFailure(LoadAttempt primary, LoadAttempt backup) {
        if (primary.missing() && backup.missing()) {
            return DialogueRepositoryLoadResult.notFound();
        }
        DiagnosticCategory category = backup.missing()
                ? primary.category()
                : DiagnosticCategory.PRIMARY_AND_BACKUP_CORRUPT;
        return DialogueRepositoryLoadResult.failed(List.of(diagnostic(
                "EDLG-PERSIST-006", category, "Snapshot сессии не восстановлен.")));
    }

    private DialogueDiagnostic diagnostic(String code, DiagnosticCategory category, String message) {
        return DialogueDiagnostic.error(code, category, message);
    }

    private record LoadAttempt(DialogueCheckpoint checkpoint, boolean missing, DiagnosticCategory category) {
        static LoadAttempt found(DialogueCheckpoint checkpoint) {
            return new LoadAttempt(checkpoint, false, null);
        }

        static LoadAttempt missingAttempt() {
            return new LoadAttempt(null, true, null);
        }

        static LoadAttempt failed(DiagnosticCategory category) {
            return new LoadAttempt(null, false, category);
        }
    }
}
