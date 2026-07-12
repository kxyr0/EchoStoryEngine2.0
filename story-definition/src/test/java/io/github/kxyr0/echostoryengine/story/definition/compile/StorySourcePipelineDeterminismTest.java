package io.github.kxyr0.echostoryengine.story.definition.compile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

final class StorySourcePipelineDeterminismTest {
    @Test
    void identicalSourceCompilesToEquivalentOutput() {
        StoryCompilationResult first = SourcePipelineFixture.compile(SourcePipelineFixture.validInput().content());
        StoryCompilationResult second = SourcePipelineFixture.compile(SourcePipelineFixture.validInput().content());

        assertEquals(first.compiledStory(), second.compiledStory());
    }

    @Test
    void jsonFieldOrderingDoesNotChangeCompiledMeaning() {
        String reordered = SourcePipelineFixture.validInput().content()
                .replace("\"formatVersion\": 1,\n  \"storyId\"", "\"storyId\"")
                .replace("\"startNode\": \"start\",", "\"startNode\": \"start\",\n  \"formatVersion\": 1,");

        StoryDefinition first = SourcePipelineFixture.compile(SourcePipelineFixture.validInput().content())
                .compiledStory()
                .orElseThrow();
        StoryDefinition second = SourcePipelineFixture.compile(reordered).compiledStory().orElseThrow();

        assertEquals(first, second);
    }

    @Test
    void sourceCollectionMutationAfterCompilationCannotAlterCompiledOutput() {
        Map<String, String> arguments = new HashMap<>();
        arguments.put("message", "ready");
        List<SourceNode> nodes = new ArrayList<>(manualNodes(arguments));
        SourceStory source = manualSource(nodes);

        StoryDefinition compiled = new StorySourceCompiler()
                .compile(source, SourcePipelineFixture.TIME)
                .compiledStory()
                .orElseThrow();
        arguments.put("message", "changed");
        nodes.clear();

        StoryNode action = compiled.find(NodeId.of("confirm")).orElseThrow();
        assertEquals("ready", action.actionArguments().get("message"));
        assertEquals(5, compiled.nodes().size());
    }

    @Test
    void expectedSourceFailuresDoNotThrowUncaughtExceptions() {
        String malformed = "{";

        assertDoesNotThrow(() -> SourcePipelineFixture.compile(malformed));
        assertDoesNotThrow(() -> SourcePipelineFixture.compile(missingTargetJson()));
    }

    @Test
    void diagnosticsContainSourceAndNodeContextWhenAvailable() {
        StoryCompilationResult result = SourcePipelineFixture.compile(missingTargetJson());

        Optional<StoryDiagnostic> diagnostic = result.diagnostics().stream()
                .filter(value -> value.code().equals("ESE-SOURCE-007"))
                .findFirst();

        assertTrue(diagnostic.isPresent());
        assertEquals(SourcePipelineFixture.SOURCE_ID, diagnostic.get().context().get("sourceId"));
        assertEquals("confirm", diagnostic.get().subject().nodeId());
    }

    private static String missingTargetJson() {
        return SourcePipelineFixture.validInput().content()
                .replace("\"next\": \"finish\"", "\"next\": \"missing\"");
    }

    private static SourceStory manualSource(List<SourceNode> nodes) {
        return new SourceStory(
                SourcePipelineFixture.SOURCE_ID,
                Optional.of(1),
                Optional.of("sample:hello_story"),
                Optional.of("start"),
                nodes,
                Set.of("formatVersion", "storyId", "startNode", "nodes"),
                SourceLocation.unknown());
    }

    private static List<SourceNode> manualNodes(Map<String, String> actionArguments) {
        return List.of(
                node("start", "start", "set_ready"),
                setNode(),
                conditionNode(),
                actionNode(actionArguments),
                node("finish", "finish", ""));
    }

    private static SourceNode setNode() {
        return new SourceNode(
                Optional.of("set_ready"),
                Optional.of("set_variable"),
                Optional.of("check_ready"),
                Optional.of("ready"),
                Optional.of("true"),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "value", "next"),
                SourceLocation.unknown());
    }

    private static SourceNode conditionNode() {
        return new SourceNode(
                Optional.of("check_ready"),
                Optional.of("condition"),
                Optional.of("confirm"),
                Optional.of("ready"),
                Optional.empty(),
                Optional.of("true"),
                Optional.empty(),
                Map.of(),
                List.of(),
                Set.of("id", "kind", "variable", "expectedValue", "next"),
                SourceLocation.unknown());
    }

    private static SourceNode actionNode(Map<String, String> arguments) {
        return new SourceNode(
                Optional.of("confirm"),
                Optional.of("external_action"),
                Optional.of("finish"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("sample:confirm"),
                arguments,
                List.of(),
                Set.of("id", "kind", "actionType", "arguments", "next"),
                SourceLocation.unknown());
    }

    private static SourceNode node(String id, String kind, String next) {
        Set<String> fields = next.isEmpty() ? Set.of("id", "kind") : new HashSet<>(Set.of("id", "kind", "next"));
        return new SourceNode(
                Optional.of(id),
                Optional.of(kind),
                next.isEmpty() ? Optional.empty() : Optional.of(next),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Map.of(),
                List.of(),
                fields,
                SourceLocation.unknown());
    }
}
