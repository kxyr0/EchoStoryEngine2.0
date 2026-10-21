package io.github.kxyr0.echostoryengine.story.forge.diagnostic.catalog;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticFactory;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeDiagnosticParts;
import java.time.Instant;
import java.util.Map;

/**
 * Diagnostics emitted while handlers start, recover and complete actions.
 */
public final class ForgeExtensionDispatchDiagnostics {
    public StoryDiagnostic startRejected(
            ExecutionId executionId,
            String actionType,
            String status,
            Instant occurredAt) {
        return diagnostic(new DispatchDiagnosticSpec("ESE-FORGE-EXT-009", DiagnosticSeverity.WARN,
                "External action start отклонён.", executionId, actionType, status, "start-rejected", occurredAt,
                "Story execution получит failure completion."));
    }

    public StoryDiagnostic startFailed(
            ExecutionId executionId,
            String actionType,
            String failureCategory,
            Instant occurredAt) {
        return diagnostic(new DispatchDiagnosticSpec("ESE-FORGE-EXT-010", DiagnosticSeverity.ERROR,
                "External action start выбросил exception.", executionId, actionType, failureCategory,
                "start-threw", occurredAt, "Exception из adapter изолирован от runtime."));
    }

    public StoryDiagnostic recoveryBlocked(ExecutionId executionId, String actionType, Instant occurredAt) {
        return diagnostic(new DispatchDiagnosticSpec("ESE-FORGE-EXT-011", DiagnosticSeverity.ERROR,
                "Recovery external action заблокирован.", executionId, actionType, "missing-provider",
                "recovery-blocked", occurredAt,
                "WAITING checkpoint сохранён; верните мод-провайдер и перезапустите сервер."));
    }

    public StoryDiagnostic recoveryRejected(
            ExecutionId executionId,
            String actionType,
            String status,
            Instant occurredAt) {
        return diagnostic(new DispatchDiagnosticSpec("ESE-FORGE-EXT-012", DiagnosticSeverity.ERROR,
                "External action recovery отклонён.", executionId, actionType, status, "recovery-rejected",
                occurredAt,
                "Adapter должен восстановить тот же ActionExecutionId или явно диагностировать состояние."));
    }

    public StoryDiagnostic completionRejected(
            ExecutionId executionId,
            String actionType,
            String acceptance,
            Instant occurredAt) {
        return diagnostic(new DispatchDiagnosticSpec("ESE-FORGE-EXT-013", DiagnosticSeverity.WARN,
                "External action completion отклонён.", executionId, actionType, acceptance,
                "completion-rejected", occurredAt,
                "Повторные, неизвестные или поздние completions не применяются к Story state."));
    }

    private StoryDiagnostic diagnostic(DispatchDiagnosticSpec spec) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(spec.code(), spec.severity(),
                "forge-extension", spec.message(), subject(spec.executionId()),
                Map.of("actionType", spec.actionType(), "detail", spec.detail()), spec.cause(),
                spec.occurredAt(), spec.hint()));
    }

    private DiagnosticSubject subject(ExecutionId executionId) {
        return new DiagnosticSubject("", executionId.asString(), "", "", executionId.asString(), "");
    }

    private record DispatchDiagnosticSpec(
            String code,
            DiagnosticSeverity severity,
            String message,
            ExecutionId executionId,
            String actionType,
            String detail,
            String cause,
            Instant occurredAt,
            String hint) {
    }
}
