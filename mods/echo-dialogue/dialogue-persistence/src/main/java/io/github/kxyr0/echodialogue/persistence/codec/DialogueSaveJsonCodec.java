package io.github.kxyr0.echodialogue.persistence.codec;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.github.kxyr0.echodialogue.api.diagnostic.DiagnosticCategory;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueNodeId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.outcome.DialogueOutcomeId;
import io.github.kxyr0.echodialogue.core.fingerprint.CompiledDialogueFingerprint;
import io.github.kxyr0.echodialogue.core.model.DialoguePresentationKind;
import io.github.kxyr0.echodialogue.core.persistence.DialogueCheckpoint;
import io.github.kxyr0.echodialogue.core.persistence.DialogueRepositoryRevision;
import io.github.kxyr0.echodialogue.core.session.DialogueSessionPhase;
import io.github.kxyr0.echodialogue.persistence.schema.DialogueSaveV1;
import io.github.kxyr0.echodialogue.persistence.schema.DialogueSaveV2;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Deterministic UTF-8 JSON codec for Dialogue Save Schema V2 with V1 migration.
 */
public final class DialogueSaveJsonCodec {
    private final Gson gson = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public byte[] encode(DialogueCheckpoint checkpoint) {
        return (gson.toJson(toSchema(checkpoint)) + "\n").getBytes(StandardCharsets.UTF_8);
    }

    public DialogueCheckpoint decode(byte[] bytes) {
        JsonObject root = readRoot(bytes);
        int version = version(root);
        if (version == DialogueSaveV1.VERSION) {
            return checkpoint(gson.fromJson(root, DialogueSaveV1.class));
        }
        if (version == DialogueSaveV2.VERSION) {
            return checkpoint(gson.fromJson(root, DialogueSaveV2.class));
        }
        throw failure(DiagnosticCategory.UNSUPPORTED_SAVE_SCHEMA, "Версия save schema не поддерживается.");
    }

    private JsonObject readRoot(byte[] bytes) {
        try {
            return gson.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonObject.class);
        } catch (JsonSyntaxException exception) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "JSON snapshot повреждён.");
        }
    }

    private int version(JsonObject root) {
        if (root == null || !root.has("saveSchemaVersion")) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "JSON snapshot пуст.");
        }
        try {
            return root.get("saveSchemaVersion").getAsInt();
        } catch (RuntimeException exception) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "Поле saveSchemaVersion повреждено.");
        }
    }

    private DialogueCheckpoint checkpoint(DialogueSaveV1 schema) {
        DialogueSessionPhase phase = phase(schema.phase());
        Optional<String> terminal = migratedTerminal(phase, schema.terminalResult());
        validateTerminal(phase, terminal, schema.cancellationReason());
        return new DialogueCheckpoint(
                new DialogueSessionId(required(schema.sessionId(), "sessionId")),
                DialogueId.parse(required(schema.dialogueId(), "dialogueId")),
                new CompiledDialogueFingerprint(required(schema.compiledFingerprint(), "compiledFingerprint")),
                new AudienceId(required(schema.audienceId(), "audienceId")),
                phase,
                schema.revision(),
                new DialogueNodeId(required(schema.currentNodeId(), "currentNodeId")),
                DialoguePresentationKind.LINE,
                schema.lineOrdinal(),
                schema.createdAtEpochMillis(),
                schema.updatedAtEpochMillis(),
                terminal,
                optional(schema.cancellationReason()),
                schema.metadata() == null ? Map.of() : schema.metadata(),
                new DialogueRepositoryRevision(schema.repositoryRevision()));
    }

    private DialogueCheckpoint checkpoint(DialogueSaveV2 schema) {
        DialogueSessionPhase phase = phase(schema.phase());
        validateTerminal(phase, optional(schema.terminalOutcomeId()), schema.cancellationReason());
        return new DialogueCheckpoint(
                new DialogueSessionId(required(schema.sessionId(), "sessionId")),
                DialogueId.parse(required(schema.dialogueId(), "dialogueId")),
                new CompiledDialogueFingerprint(required(schema.compiledFingerprint(), "compiledFingerprint")),
                new AudienceId(required(schema.audienceId(), "audienceId")),
                phase,
                schema.revision(),
                new DialogueNodeId(required(schema.currentNodeId(), "currentNodeId")),
                presentationKind(schema.presentationKind()),
                schema.lineOrdinal(),
                schema.createdAtEpochMillis(),
                schema.updatedAtEpochMillis(),
                optional(schema.terminalOutcomeId()),
                optional(schema.cancellationReason()),
                schema.metadata() == null ? Map.of() : schema.metadata(),
                new DialogueRepositoryRevision(schema.repositoryRevision()));
    }

    private DialogueSaveV2 toSchema(DialogueCheckpoint checkpoint) {
        return new DialogueSaveV2(
                DialogueSaveV2.VERSION,
                checkpoint.sessionId().toString(),
                checkpoint.dialogueId().toString(),
                checkpoint.compiledFingerprint().toString(),
                checkpoint.audienceId().toString(),
                checkpoint.phase().name(),
                checkpoint.revision(),
                checkpoint.currentNodeId().toString(),
                checkpoint.presentationKind().name(),
                checkpoint.lineOrdinal(),
                checkpoint.createdAtEpochMillis(),
                checkpoint.updatedAtEpochMillis(),
                checkpoint.terminalResult().orElse(null),
                checkpoint.cancellationReason().orElse(null),
                new TreeMap<>(checkpoint.metadata()),
                checkpoint.repositoryRevision().value());
    }

    private Optional<String> migratedTerminal(DialogueSessionPhase phase, String raw) {
        if (phase != DialogueSessionPhase.COMPLETED) {
            return optional(raw);
        }
        if (blank(raw) || raw.equals("completed")) {
            return Optional.of(DialogueOutcomeId.DEFAULT.toString());
        }
        return Optional.of(DialogueOutcomeId.parse(raw).toString());
    }

    private void validateTerminal(DialogueSessionPhase phase, Optional<String> terminal, String cancellationReason) {
        if (phase == DialogueSessionPhase.COMPLETED && terminal.isEmpty()) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "COMPLETED snapshot не содержит terminalOutcomeId.");
        }
        terminal.ifPresent(DialogueOutcomeId::parse);
        if (phase == DialogueSessionPhase.CANCELLED && blank(cancellationReason)) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "CANCELLED snapshot не содержит cancellationReason.");
        }
    }

    private DialogueSessionPhase phase(String value) {
        try {
            return DialogueSessionPhase.valueOf(required(value, "phase"));
        } catch (IllegalArgumentException exception) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "Snapshot содержит неизвестную phase.");
        }
    }

    private String required(String value, String field) {
        if (blank(value)) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED, "Snapshot не содержит поле " + field + ".");
        }
        return value.trim();
    }

    private Optional<String> optional(String value) {
        return blank(value) ? Optional.empty() : Optional.of(value.trim());
    }

    private DialoguePresentationKind presentationKind(String value) {
        try {
            return DialoguePresentationKind.valueOf(required(value, "presentationKind"));
        } catch (IllegalArgumentException exception) {
            throw failure(DiagnosticCategory.SNAPSHOT_MALFORMED,
                    "Snapshot содержит неизвестный presentationKind.");
        }
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private SaveCodecException failure(DiagnosticCategory category, String message) {
        return new SaveCodecException(category, message);
    }

    public static final class SaveCodecException extends RuntimeException {
        private final DiagnosticCategory category;

        SaveCodecException(DiagnosticCategory category, String message) {
            super(message);
            this.category = category;
        }

        public DiagnosticCategory category() {
            return category;
        }
    }
}
