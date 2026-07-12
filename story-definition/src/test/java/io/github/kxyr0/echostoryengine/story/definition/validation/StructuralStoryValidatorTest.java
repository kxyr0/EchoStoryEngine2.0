package io.github.kxyr0.echostoryengine.story.definition.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.parse.JsonStorySourceParser;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

final class StructuralStoryValidatorTest {
    @Test
    void validSourcePassesStructuralValidation() {
        StoryValidationResult result = validate(validJson());

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void unsupportedFormatVersionIsRejected() {
        StoryValidationResult result = validate(validJson().replace("\"formatVersion\": 1", "\"formatVersion\": 2"));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-002");
    }

    @Test
    void missingStoryIdIsRejected() {
        StoryValidationResult result = validate(validJson().replace("  \"storyId\": \"sample:hello_story\",\n", ""));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "storyId");
    }

    @Test
    void invalidNamespacedStoryIdIsRejected() {
        StoryValidationResult result = validate(validJson().replace("sample:hello_story", "invalid story"));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-004");
    }

    @Test
    void missingStartNodeFieldIsRejected() {
        StoryValidationResult result = validate(validJson().replace("  \"startNode\": \"start\",\n", ""));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "startNode");
    }

    @Test
    void duplicateNodeIdsAreRejected() {
        StoryValidationResult result = validate(validJson().replace("\"id\": \"finish\"", "\"id\": \"confirm\""));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-005");
    }

    @Test
    void invalidNodeKindIsRejected() {
        StoryValidationResult result = validate(validJson().replace("\"kind\": \"finish\"", "\"kind\": \"unknown\""));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-010");
    }

    @Test
    void missingRequiredNodeFieldIsRejected() {
        String invalid = validJson().replace(
                "      \"kind\": \"start\",\n      \"next\": \"set_ready\"",
                "      \"kind\": \"start\"");

        StoryValidationResult result = validate(invalid);

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-003", "field", "next");
    }

    @Test
    void nonStringActionArgumentIsRejected() {
        StoryValidationResult result = validate(validJson().replace("\"message\": \"ready\"", "\"message\": 7"));

        assertFalse(result.succeeded());
        assertHasContext(result.diagnostics(), "ESE-SOURCE-012", "field", "arguments");
    }

    private static StoryValidationResult validate(String content) {
        SourceStory source = parse(content);
        return new StructuralStoryValidator().validate(source, SourcePipelineFixture.TIME);
    }

    private static SourceStory parse(String content) {
        StorySourceInput input = new StorySourceInput(SourcePipelineFixture.SOURCE_ID, content);
        return new JsonStorySourceParser().parse(input, SourcePipelineFixture.TIME).source().orElseThrow();
    }

    private static String validJson() {
        return SourcePipelineFixture.validInput().content();
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
