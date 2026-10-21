package io.github.kxyr0.echostoryengine.story.forge.extension.readmodel;

import java.util.List;
import java.util.Objects;

/**
 * Detailed read model for one registered or rejected extension.
 */
public record ExtensionDetail(
        ExtensionSummary summary,
        String rejectionReason,
        List<String> actionLines) {
    public ExtensionDetail {
        summary = Objects.requireNonNull(summary, "summary");
        rejectionReason = Objects.requireNonNull(rejectionReason, "rejectionReason");
        actionLines = List.copyOf(Objects.requireNonNull(actionLines, "actionLines"));
    }
}
