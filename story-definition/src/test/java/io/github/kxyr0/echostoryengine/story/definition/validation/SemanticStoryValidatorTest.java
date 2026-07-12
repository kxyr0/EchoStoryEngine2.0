package io.github.kxyr0.echostoryengine.story.definition.validation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.parse.JsonStorySourceParser;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceStory;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

final class SemanticStoryValidatorTest {
    @Test
    void validSourcePassesSemanticValidation() {
        StoryValidationResult result = validate(validJson());

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().isEmpty());
    }

    @Test
    void missingTransitionTargetIsRejected() {
        StoryValidationResult result = validate(validJson().replace("\"next\": \"finish\"", "\"next\": \"missing\""));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-007");
    }

    @Test
    void unreachableNodeIsReportedAsWarning() {
        StoryValidationResult result = validate(withUnusedNode());

        assertTrue(result.succeeded());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals("ESE-SOURCE-008")
                && diagnostic.severity() == DiagnosticSeverity.WARN));
    }

    @Test
    void synchronousCycleIsRejected() {
        StoryValidationResult result = validate(validJson().replace("\"next\": \"set_ready\"", "\"next\": \"start\""));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-009");
    }

    @Test
    void invalidActionTypeIsRejected() {
        StoryValidationResult result = validate(validJson().replace("sample:confirm", "invalid action"));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-004");
    }

    @Test
    void missingStartNodeReferenceIsRejected() {
        StoryValidationResult result = validate(validJson()
                .replace("\"startNode\": \"start\"", "\"startNode\": \"absent\""));

        assertFalse(result.succeeded());
        assertHasCode(result.diagnostics(), "ESE-SOURCE-006");
    }

    private static StoryValidationResult validate(String content) {
        SourceStory source = parse(content);
        return new SemanticStoryValidator().validate(source, SourcePipelineFixture.TIME);
    }

    private static SourceStory parse(String content) {
        StorySourceInput input = new StorySourceInput(SourcePipelineFixture.SOURCE_ID, content);
        return new JsonStorySourceParser().parse(input, SourcePipelineFixture.TIME).source().orElseThrow();
    }

    private static String withUnusedNode() {
        return validJson().replace(
                "    {\n      \"id\": \"finish\",\n      \"kind\": \"finish\"\n    }\n",
                "    {\n      \"id\": \"finish\",\n      \"kind\": \"finish\"\n    },\n"
                        + "    {\n      \"id\": \"unused\",\n      \"kind\": \"finish\"\n    }\n");
    }

    private static String validJson() {
        return SourcePipelineFixture.validInput().content();
    }

    private static void assertHasCode(List<StoryDiagnostic> diagnostics, String code) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }
}
