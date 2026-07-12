package io.github.kxyr0.echostoryengine.story.definition.diagnostic;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSubject;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Source context used to create structured diagnostics.
 */
public record SourceDiagnosticContext(
        String sourceId,
        String storyId,
        String nodeId,
        SourceLocation location,
        Map<String, String> context) {
    public SourceDiagnosticContext {
        sourceId = Objects.requireNonNull(sourceId, "sourceId");
        storyId = Objects.requireNonNull(storyId, "storyId");
        nodeId = Objects.requireNonNull(nodeId, "nodeId");
        location = Objects.requireNonNull(location, "location");
        context = Map.copyOf(Objects.requireNonNull(context, "context"));
    }

    public static SourceDiagnosticContext fromInput(
            StorySourceInput input,
            SourceLocation location,
            Map<String, String> context) {
        return new SourceDiagnosticContext(input.sourceId(), "", "", location, context);
    }

    public static SourceDiagnosticContext fromStory(SourceStory story, Map<String, String> context) {
        return new SourceDiagnosticContext(
                story.sourceId(),
                story.storyId().orElse(""),
                "",
                story.location(),
                context);
    }

    public static SourceDiagnosticContext fromNode(
            SourceStory story,
            SourceNode node,
            Map<String, String> context) {
        return new SourceDiagnosticContext(
                story.sourceId(),
                story.storyId().orElse(""),
                node.id().orElse(""),
                node.location(),
                context);
    }

    public DiagnosticSubject subject() {
        return new DiagnosticSubject(storyId, "", nodeId, "", sourceId, location.asSubjectValue());
    }

    public Map<String, String> boundedContext() {
        LinkedHashMap<String, String> values = new LinkedHashMap<>();
        values.put("sourceId", sourceId);
        if (location.isKnown()) {
            values.put("line", Integer.toString(location.line()));
            values.put("column", Integer.toString(location.column()));
        }
        values.putAll(context);
        return Map.copyOf(values);
    }
}
