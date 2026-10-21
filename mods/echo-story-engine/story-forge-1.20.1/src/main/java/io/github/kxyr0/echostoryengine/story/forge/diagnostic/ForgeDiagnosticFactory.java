package io.github.kxyr0.echostoryengine.story.forge.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;

public final class ForgeDiagnosticFactory {
    private ForgeDiagnosticFactory() {
    }

    public static StoryDiagnostic create(ForgeDiagnosticParts parts) {
        return new StoryDiagnostic(
                parts.code(),
                parts.severity(),
                parts.subsystem(),
                parts.message(),
                parts.subject(),
                parts.context(),
                parts.causeCategory(),
                parts.occurredAt(),
                parts.operatorHint());
    }
}
