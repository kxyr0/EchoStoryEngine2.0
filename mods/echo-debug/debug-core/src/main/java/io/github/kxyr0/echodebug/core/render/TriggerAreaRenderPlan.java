package io.github.kxyr0.echodebug.core.render;

import io.github.kxyr0.echotrigger.api.geometry.AreaShapeKind;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import java.util.List;
import java.util.Objects;

public record TriggerAreaRenderPlan(
        TriggerId triggerId,
        AreaShapeKind shapeKind,
        String dimension,
        List<RenderLine> lines,
        List<RenderLabel> labels,
        int sourceParserCalls
) {
    public TriggerAreaRenderPlan {
        triggerId = Objects.requireNonNull(triggerId, "triggerId");
        shapeKind = Objects.requireNonNull(shapeKind, "shapeKind");
        dimension = Objects.requireNonNull(dimension, "dimension").trim();
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
        if (dimension.isEmpty() || sourceParserCalls != 0) {
            throw new IllegalArgumentException("Render plan requires dimension and zero parser calls.");
        }
    }
}
