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
 * Diagnostics emitted around abstract external action dispatch.
 */
public final class ForgeActionDiagnostics {
    public StoryDiagnostic commandHandlerMissing(ExecutionId executionId, String actionType, Instant occurredAt) {
        return ForgeDiagnosticFactory.create(new ForgeDiagnosticParts(
                "ESE-FORGE-ACTION-001",
                DiagnosticSeverity.WARN,
                "forge-action",
                "External command was saved but no production handler is connected.",
                new DiagnosticSubject("", executionId.asString(), "", "", executionId.asString(), ""),
                Map.of("actionType", actionType),
                "missing-action-handler",
                occurredAt,
                "M4 only diagnoses abstract actions; handler integration belongs to M5."));
    }
}
