package io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticParts;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted while external extensions register and create sessions.
 */
public final class ForgeExtensionRegistrationDiagnostics {
    public StoryDiagnostic registrationOpened(Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-001", DiagnosticSeverity.INFO,
                "Регистрация Story extensions открыта.", Map.of(), "registration-opened", occurredAt,
                "Внешние моды могут зарегистрировать factory."));
    }

    public StoryDiagnostic registrationClosed(int factoryCount, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-002", DiagnosticSeverity.INFO,
                "Регистрация Story extensions закрыта.", Map.of("factoryCount", Integer.toString(factoryCount)),
                "registration-closed", occurredAt, "Поздняя регистрация в этот host не принимается."));
    }

    public StoryDiagnostic descriptorFailed(String failureCategory, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-003", DiagnosticSeverity.ERROR,
                "Extension descriptor не прочитан.", Map.of("failureCategory", failureCategory),
                "descriptor-failed", occurredAt, "Проверьте exception в моде-провайдере extension."));
    }

    public StoryDiagnostic accepted(String extensionId, int actionCount, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-004", DiagnosticSeverity.INFO,
                "Story extension принята.", withExtension(Map.of("actionCount", Integer.toString(actionCount)),
                extensionId), "extension-accepted", occurredAt,
                "Extension доступна для dispatch внешних Story actions."));
    }

    public StoryDiagnostic incompatible(String extensionId, String requiredApi, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-005", DiagnosticSeverity.ERROR,
                "Story extension отклонена по версии API.",
                withExtension(Map.of("requiredApi", requiredApi), extensionId), "incompatible-api-version",
                occurredAt, "Используйте совместимую версию Story Extension API."));
    }

    public StoryDiagnostic duplicateExtension(String extensionId, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-006", DiagnosticSeverity.ERROR,
                "Duplicate Story extension id.", withExtension(Map.of(), extensionId), "duplicate-extension-id",
                occurredAt, "Оставьте только один мод с этим extension id."));
    }

    public StoryDiagnostic duplicateAction(String actionType, String extensionId, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-007", DiagnosticSeverity.ERROR,
                "Duplicate external action type.", withExtension(Map.of("actionType", actionType), extensionId),
                "duplicate-action-type", occurredAt,
                "Action type недоступен, пока у него больше одного owner."));
    }

    public StoryDiagnostic sessionFailed(String extensionId, String failureCategory, Instant occurredAt) {
        return diagnostic(new RegistrationDiagnosticSpec("ESE-FORGE-EXT-008", DiagnosticSeverity.ERROR,
                "Story extension session не создана.",
                withExtension(Map.of("failureCategory", failureCategory), extensionId),
                "session-create-failed", occurredAt, "Исправьте factory внешнего мода."));
    }

    private StoryDiagnostic diagnostic(RegistrationDiagnosticSpec spec) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(spec.code(), spec.severity(),
                "forge-extension", spec.message(), new DiagnosticSubject("", "", "", "", "", ""),
                spec.context(), spec.cause(), spec.occurredAt(), spec.hint()));
    }

    private Map<String, String> withExtension(Map<String, String> context, String extensionId) {
        java.util.HashMap<String, String> result = new java.util.HashMap<>(context);
        result.put("extensionId", extensionId);
        return Map.copyOf(result);
    }

    private record RegistrationDiagnosticSpec(
            String code,
            DiagnosticSeverity severity,
            String message,
            Map<String, String> context,
            String cause,
            Instant occurredAt,
            String hint) {
    }
}
