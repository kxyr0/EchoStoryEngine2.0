package io.github.kxyr0.echostoryengine.story.definition.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class SemanticStoryValidatorTest {
    @Test
    void validSourcePassesSemanticValidation() {
        StoryValidationResult result = validate(SourcePipelineFixture.validSource());

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void missingTransitionTargetIsRejected() {
        List<SourceNode> nodes = SourcePipelineFixture.validNodes().stream()
                .map(node -> node.id().filter("confirm"::equals).isPresent()
                        ? SourcePipelineFixture.actionNode("confirm", "sample:confirm", "missing", java.util.Map.of())
                        : node)
                .toList();
        StoryValidationResult result = validate(source("start", nodes));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-007");
    }

    @Test
    void unreachableNodeIsReportedAsWarning() {
        List<SourceNode> nodes = new ArrayList<>(SourcePipelineFixture.validNodes());
        nodes.add(SourcePipelineFixture.node("unused", "finish", Optional.empty()));
        StoryValidationResult result = validate(source("start", nodes));

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("ESE-SOURCE-008")
                && diagnostic.severity() == DiagnosticSeverity.WARN));
    }

    @Test
    void synchronousCycleIsRejected() {
        List<SourceNode> nodes = SourcePipelineFixture.validNodes().stream()
                .map(node -> node.id().filter("start"::equals).isPresent()
                        ? SourcePipelineFixture.node("start", "start", Optional.of("start"))
                        : node)
                .toList();
        StoryValidationResult result = validate(source("start", nodes));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-009");
    }

    @Test
    void invalidActionTypeIsRejected() {
        List<SourceNode> nodes = SourcePipelineFixture.validNodes().stream()
                .map(node -> node.id().filter("confirm"::equals).isPresent()
                        ? SourcePipelineFixture.actionNode("confirm", "invalid action", "finish", java.util.Map.of())
                        : node)
                .toList();
        StoryValidationResult result = validate(source("start", nodes));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-004");
    }

    @Test
    void missingStartNodeReferenceIsRejected() {
        StoryValidationResult result = validate(source("absent", SourcePipelineFixture.validNodes()));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-006");
    }

    private static StoryValidationResult validate(SourceStory source) {
        return new SemanticStoryValidator().validate(source, SourcePipelineFixture.TIME);
    }

    private static SourceStory source(String startNode, List<SourceNode> nodes) {
        return SourcePipelineFixture.source(
                Optional.of(1),
                Optional.of("sample:hello_story"),
                Optional.of(startNode),
                nodes);
    }

    private static void assertHasCode(List<StoryDiagnostic> diagnostics, String code) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }
}
