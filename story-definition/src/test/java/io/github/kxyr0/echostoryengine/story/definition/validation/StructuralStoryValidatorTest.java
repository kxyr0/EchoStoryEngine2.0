package io.github.kxyr0.echostoryengine.story.definition.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class StructuralStoryValidatorTest {
    @Test
    void validSourcePassesStructuralValidation() {
        StoryValidationResult result = validate(SourcePipelineFixture.validSource());

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void unsupportedFormatVersionIsRejected() {
        StoryValidationResult result = validate(source(Optional.of(2), Optional.of("sample:hello_story"),
                Optional.of("start"), SourcePipelineFixture.validNodes()));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-002");
    }

    @Test
    void missingStoryIdIsRejected() {
        StoryValidationResult result = validate(source(Optional.of(1), Optional.empty(), Optional.of("start"),
                SourcePipelineFixture.validNodes()));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "storyId");
    }

    @Test
    void invalidNamespacedStoryIdIsRejected() {
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("invalid story"),
                Optional.of("start"), SourcePipelineFixture.validNodes()));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-004");
    }

    @Test
    void missingStartNodeFieldIsRejected() {
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("sample:hello_story"),
                Optional.empty(), SourcePipelineFixture.validNodes()));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "startNode");
    }

    @Test
    void duplicateNodeIdsAreRejected() {
        List<SourceNode> nodes = SourcePipelineFixture.validNodes().stream()
                .map(node -> node.id().filter("finish"::equals).isPresent()
                        ? SourcePipelineFixture.node("confirm", "finish", Optional.empty())
                        : node)
                .toList();
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("sample:hello_story"),
                Optional.of("start"), nodes));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-005");
    }

    @Test
    void invalidNodeKindIsRejected() {
        List<SourceNode> nodes = List.of(SourcePipelineFixture.node("start", "unknown", Optional.empty()));
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("sample:hello_story"),
                Optional.of("start"), nodes));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-010");
    }

    @Test
    void missingRequiredNodeFieldIsRejected() {
        List<SourceNode> nodes = List.of(SourcePipelineFixture.node("start", "start", Optional.empty()));
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("sample:hello_story"),
                Optional.of("start"), nodes));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "next");
    }

    @Test
    void invalidActionArgumentIsRejected() {
        SourceNode invalid = new SourceNode(
                Optional.of("confirm"),
                Optional.of("external_action"),
                Optional.of("finish"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.of("sample:confirm"),
                java.util.Map.of(),
                List.of("message"),
                java.util.Set.of("id", "kind", "actionType", "arguments", "next"),
                io.github.kxyr0.echostoryengine.story.definition.source.SourceLocation.unknown());
        StoryValidationResult result = validate(source(Optional.of(1), Optional.of("sample:hello_story"),
                Optional.of("confirm"), List.of(invalid)));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-012", "field", "arguments");
    }

    private static StoryValidationResult validate(SourceStory source) {
        return new StructuralStoryValidator().validate(source, SourcePipelineFixture.TIME);
    }

    private static SourceStory source(
            Optional<Integer> formatVersion,
            Optional<String> storyId,
            Optional<String> startNode,
            List<SourceNode> nodes) {
        return SourcePipelineFixture.source(formatVersion, storyId, startNode, nodes);
    }

    private static void assertHasCode(List<StoryDiagnostic> diagnostics, String code) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }

    private static void assertHasContext(
            List<StoryDiagnostic> diagnostics,
            String code,
            String key,
            String value) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().equals(code)
                && value.equals(diagnostic.context().get(key))));
    }
}
