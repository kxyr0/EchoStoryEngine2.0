package io.github.kxyr0.echotrigger.api.authoring.source;

import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.Objects;

public record TriggerSourceValidationRequest(
        TriggerId expectedId,
        String sourceName,
        String sourceText
) {
    public TriggerSourceValidationRequest {
        expectedId = Objects.requireNonNull(expectedId, "expectedId");
        sourceName = Objects.requireNonNull(sourceName, "sourceName").trim();
        sourceText = Objects.requireNonNull(sourceText, "sourceText");
        if (sourceName.isEmpty()) {
            throw new IllegalArgumentException("Source name is required.");
        }
    }
}
