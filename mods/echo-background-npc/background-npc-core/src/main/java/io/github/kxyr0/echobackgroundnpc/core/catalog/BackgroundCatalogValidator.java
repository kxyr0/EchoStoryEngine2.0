package io.github.kxyr0.echobackgroundnpc.core.catalog;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnostic.Severity;
import io.github.kxyr0.echobackgroundnpc.api.diagnostic.BackgroundDiagnosticCode;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BackgroundCatalogValidator {
    public List<BackgroundDiagnostic> validate(BackgroundCatalog catalog) {
        List<BackgroundDiagnostic> diagnostics = new ArrayList<>();
        catalog.definitions().values().forEach(value -> validate(value, catalog, diagnostics));
        return List.copyOf(diagnostics);
    }

    private void validate(BackgroundNpcDefinition value, BackgroundCatalog catalog,
            List<BackgroundDiagnostic> diagnostics) {
        value.profile().filter(id -> !catalog.profiles().containsKey(id)).ifPresent(id -> diagnostics.add(error(
                BackgroundDiagnosticCode.BG_NPC_PROFILE_NOT_FOUND, "Профиль не найден: " + id, value)));
        value.appearancePool().filter(id -> !catalog.appearancePools().containsKey(id)).ifPresent(id ->
                diagnostics.add(error(BackgroundDiagnosticCode.BG_NPC_APPEARANCE_NOT_FOUND,
                        "Пул внешности не найден: " + id, value)));
        value.schedule().stream().map(entry -> entry.task()).filter(BackgroundTask.Converse.class::isInstance)
                .map(BackgroundTask.Converse.class::cast).forEach(task -> {
                    if (!catalog.definitions().containsKey(task.participant())) {
                        diagnostics.add(error(BackgroundDiagnosticCode.BG_NPC_CONVERSATION_PARTICIPANT_UNAVAILABLE,
                                "Участник не найден: " + task.participant(), value));
                    }
                    if (!catalog.dialogues().containsKey(task.dialogue())) {
                        diagnostics.add(error(BackgroundDiagnosticCode.BG_NPC_CONVERSATION_NOT_FOUND,
                                "Ambient dialogue не найден: " + task.dialogue(), value));
                    }
                });
    }

    private BackgroundDiagnostic error(BackgroundDiagnosticCode code, String message,
            BackgroundNpcDefinition definition) {
        return new BackgroundDiagnostic(code, Severity.ERROR, message, Optional.of(definition.source()));
    }
}
