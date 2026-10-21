package io.github.kxyr0.echostoryengine.story.definition;

import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class SourcePipelineFixture {
    public static final Instant TIME = Instant.parse("2026-07-12T00:00:00Z");
    public static final String SOURCE_ID = "test:valid-story.es";

    private SourcePipelineFixture() {
    }

    public static StorySourceInput validInput() {
        return new StorySourceInput(SOURCE_ID, "");
    }

    public static SourceStory validSource() {
        return source(Optional.of(1), Optional.of("sample:hello_story"), Optional.of("start"), validNodes());
    }

    public static SourceStory source(
            Optional<Integer> formatVersion,
            Optional<String> storyId,
            Optional<String> startNode,
            List<SourceNode> nodes) {
        return new SourceStory(
                SOURCE_ID,
                formatVersion,
                storyId,
                startNode,
                nodes,
                Set.of("formatVersion", "storyId", "startNode", "nodes"),
                SourceLocation.unknown());
    }

    public static List<SourceNode> validNodes() {
        return List.of(
                node("start", "start", Optional.of("set_ready")),
                setNode("set_ready", "sample.ready", "true", "check_ready"),
                conditionNode("check_ready", "sample.ready", "true", "confirm"),
                actionNode("confirm", "sample:confirm", "finish", Map.of()),
                node("finish", "finish", Optional.empty()));
    }

    public static SourceNode node(String id, String kind, Optional<String> next) {
        Set<String> fields = next.isPresent() ? Set.of("id", "kind", "next") : Set.of("id", "kind");
        return new SourceNode(
                Optional.of(id),
                Optional.of(kind),
                next,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                fields,
                SourceLocation.unknown());
    }

    public static SourceNode setNode(String id, String variable, String value, String next) {
        return new SourceNode(
                Optional.of(id),
                Optional.of("set_variable"),
                Optional.of(next),
                Optional.of(variable),
                Optional.of(value),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "value", "next"),
                SourceLocation.unknown());
    }

    public static SourceNode conditionNode(String id, String variable, String expected, String next) {
        return new SourceNode(
                Optional.of(id),
                Optional.of("condition"),
                Optional.of(next),
                Optional.of(variable),
                Optional.empty(),
                Optional.of(expected),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "expectedValue", "next"),
                SourceLocation.unknown());
    }

    public static SourceNode actionNode(String id, String actionType, String next, Map<String, String> arguments) {
        Set<String> fields = arguments.isEmpty()
                ? Set.of("id", "kind", "actionType", "next")
                : Set.of("id", "kind", "actionType", "arguments", "next");
        return new SourceNode(
                Optional.of(id),
                Optional.of("external_action"),
                Optional.of(next),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(actionType),
                Optional.empty(),
                arguments,
                List.of(),
                fields,
                SourceLocation.unknown());
    }

    public static SourceNode actionNodeWithOutput(String id, String actionType, String output, String next) {
        return new SourceNode(
                Optional.of(id),
                Optional.of("external_action"),
                Optional.of(next),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of(actionType),
                Optional.of(output),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "actionType", "output", "next"),
                SourceLocation.unknown());
    }
}
