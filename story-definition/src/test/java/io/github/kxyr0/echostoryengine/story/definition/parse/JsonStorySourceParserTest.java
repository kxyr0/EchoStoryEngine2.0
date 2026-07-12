package io.github.kxyr0.echostoryengine.story.definition.parse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.definition.SourcePipelineFixture;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import org.junit.jupiter.api.Test;

final class JsonStorySourceParserTest {
    @Test
    void validJsonStoryParsesSuccessfully() {
        StoryParseResult result = new JsonStorySourceParser().parse(
                SourcePipelineFixture.validInput(),
                SourcePipelineFixture.TIME);

        assertTrue(result.succeeded());
        assertEquals(1, result.formatVersion().orElseThrow());
        assertEquals(5, result.source().orElseThrow().nodes().size());
    }

    @Test
    void malformedJsonProducesSyntaxDiagnostic() {
        StorySourceInput input = new StorySourceInput(SourcePipelineFixture.SOURCE_ID, "{\n  \"formatVersion\": 1,");

        StoryParseResult result = new JsonStorySourceParser().parse(input, SourcePipelineFixture.TIME);

        assertFalse(result.succeeded());
        assertTrue(result.source().isEmpty());
        assertEquals("ESE-SOURCE-001", result.diagnostics().get(0).code());
        assertEquals(SourcePipelineFixture.SOURCE_ID, result.diagnostics().get(0).context().get("sourceId"));
    }
}
