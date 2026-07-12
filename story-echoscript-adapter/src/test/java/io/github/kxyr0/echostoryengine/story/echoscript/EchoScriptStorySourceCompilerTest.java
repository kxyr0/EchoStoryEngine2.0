package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeResult;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.application.StoryRuntimeUseCase;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EchoScriptStorySourceCompilerTest {
    @Test
    void minimalStoryResourceCompilesIntoRuntimeModel() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile(
                "fixtures/minimal_story.es",
                EchoScriptAdapterFixture.minimalStory());

        assertTrue(result.compilationSucceeded());
        assertTrue(result.diagnostics().isEmpty());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode set = story.find(NodeId.of("set_ready")).orElseThrow();
        StoryNode require = story.find(NodeId.of("require_ready")).orElseThrow();
        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals(EchoScriptAdapterFixture.STORY_ID, story.storyId());
        assertEquals(EchoScriptAdapterFixture.START_NODE, story.startNode());
        assertEquals("sample.ready", set.variableName());
        assertEquals("true", set.variableValue());
        assertEquals("sample.ready", require.variableName());
        assertEquals("true", require.variableValue());
        assertEquals(EchoScriptAdapterFixture.ACTION_TYPE, action.actionType());
        assertTrue(story.find(EchoScriptAdapterFixture.FINISH_NODE).isPresent());
        assertThrows(UnsupportedOperationException.class, () -> story.nodes().clear());
    }

    @Test
    void compiledStoryExecutesThroughRuntimeAndRejectsDuplicateOrUnknownCompletionSafely() {
        StoryDefinition story = EchoScriptAdapterFixture.compiledMinimalStory();
        var repository = new InMemoryRepository();
        StoryRuntimeUseCase runtime = EchoScriptAdapterFixture.runtime(story, repository);

        StoryRuntimeResult waiting = runtime.advance(EchoScriptAdapterFixture.startInput());
        StoryRuntimeResult unknown = runtime.advance(EchoScriptAdapterFixture.unknownCompletionInput());
        StoryRuntimeResult finished = runtime.advance(EchoScriptAdapterFixture.successInput());
        StoryRuntimeResult duplicate = runtime.advance(EchoScriptAdapterFixture.successInput());

        assertEquals(StoryExecutionPhase.WAITING, waiting.state().phase());
        assertEquals(EchoScriptAdapterFixture.ACTION_TYPE, waiting.commands().get(0).actionType());
        assertEquals(EchoScriptAdapterFixture.ACTION_ID, waiting.commands().get(0).actionExecutionId());
        assertEquals("true", waiting.state().variables().get("sample.ready"));
        assertEquals(StoryExecutionPhase.WAITING, unknown.state().phase());
        assertEquals("ESE-ACTION-004", unknown.diagnostics().get(0).code());
        assertEquals(StoryExecutionPhase.FINISHED, finished.state().phase());
        assertEquals("ESE-ACTION-003", duplicate.diagnostics().get(0).code());
    }

    @Test
    void equivalentSyntaxDoesNotChangeFingerprintButMeaningChangesDo() {
        StoryDefinition baseline = compile(EchoScriptAdapterFixture.minimalStory());
        StoryDefinition withTrivia = compile(EchoScriptAdapterFixture.minimalStory()
                .replace("class MinimalStory", "// comment\r\nclass MinimalStory")
                .replace("\n", "\r\n"));
        StoryDefinition reordered = compile(reorderedMinimalStory());
        StoryDefinition changedAction = compile(EchoScriptAdapterFixture.minimalStory()
                .replace("sample:confirm", "sample:confirm_changed"));
        StoryDefinition changedVariable = compile(EchoScriptAdapterFixture.minimalStory()
                .replace("sample.ready", "sample.changed"));

        assertEquals(fingerprint(baseline), fingerprint(withTrivia));
        assertEquals(fingerprint(baseline), fingerprint(reordered));
        assertFalse(fingerprint(baseline).equals(fingerprint(changedAction)));
        assertFalse(fingerprint(baseline).equals(fingerprint(changedVariable)));
        StoryDefinition otherPath = compile("other/path.es", EchoScriptAdapterFixture.minimalStory());
        assertEquals(fingerprint(baseline), fingerprint(otherPath));
    }

    @Test
    void dialogueResourceIsRejectedByStoryBinderWithoutDialogueSemantics() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile(
                "fixtures/chapter_1_home.es",
                EchoScriptAdapterFixture.dialogueStory());

        assertFalse(result.compilationSucceeded());
        assertTrue(result.compiledStory().isEmpty());
        assertEquals(List.of("ESE-SOURCE-014"), result.diagnostics().stream().map(StoryDiagnostic::code).toList());
        assertTrue(EchoScriptAdapterFixture.dialogueStory().contains("Стоять"));
    }

    @Test
    void syntaxAndSemanticFailuresReturnDiagnosticsWithoutCompiledStory() {
        assertHasCode(compileResult("class A : StoryResource {"), "ESE-SOURCE-001");
        assertHasCode(new EchoScriptStorySourceCompiler(2).compile(new StorySourceInput("v2.es",
                EchoScriptAdapterFixture.minimalStory()), EchoScriptAdapterFixture.TIME), "ESE-SOURCE-002");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("Build", "MissingBuild")),
                "ESE-SOURCE-017");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("StoryResourceDef", "void")),
                "ESE-SOURCE-018");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory()
                .replace("private void Nodes()", "private Unknown field; private void Nodes()")), "ESE-SOURCE-020");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("Action(", "Unknown(")),
                "ESE-SOURCE-021");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("Finish(\"finish\")",
                "Finish(\"finish\", \"extra\")")), "ESE-SOURCE-022");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("Finish(\"finish\")",
                "Finish(\"finish\", bad: \"x\")")), "ESE-SOURCE-023");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("type: \"sample:confirm\"",
                "type: true")), "ESE-SOURCE-024");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("sample:hello_story",
                "invalid story")), "ESE-SOURCE-004");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("Finish(\"finish\")",
                "Finish(\"confirm\")")), "ESE-SOURCE-005");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("start: \"start\"",
                "start: \"missing\"")), "ESE-SOURCE-006");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("next: \"finish\"",
                "next: \"missing\"")), "ESE-SOURCE-007");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace("next: \"set_ready\"",
                "next: \"start\"")), "ESE-SOURCE-009");
        assertHasCode(compileResult(EchoScriptAdapterFixture.minimalStory().replace(
                "        Finish(\"finish\");",
                "")), "ESE-SOURCE-013");
    }

    private static StoryCompilationResult compileResult(String source) {
        return EchoScriptAdapterFixture.compile("inline.es", source);
    }

    private static StoryDefinition compile(String source) {
        return compile("fixtures/minimal_story.es", source);
    }

    private static StoryDefinition compile(String sourceId, String source) {
        return EchoScriptAdapterFixture.compile(sourceId, source).compiledStory().orElseThrow();
    }

    private static String reorderedMinimalStory() {
        return """
                namespace EchoStoryEngine.Story.Resources;

                class MinimalStory : StoryResource
                {
                    private void Nodes()
                    {
                        Finish("finish");
                        Action("confirm", type: "sample:confirm", next: "finish");
                        Require("require_ready", variable: "sample.ready", value: true, next: "confirm");
                        Set("set_ready", variable: "sample.ready", value: true, next: "require_ready");
                        Start("start", next: "set_ready");
                    }

                    public override StoryResourceDef Build()
                    {
                        return Story("sample:hello_story", start: "start");
                    }
                }
                """;
    }

    private static String fingerprint(StoryDefinition story) {
        return StoryDefinitionFingerprint.from(story).value();
    }

    private static void assertHasCode(StoryCompilationResult result, String code) {
        assertFalse(result.compilationSucceeded());
        assertTrue(result.compiledStory().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }
}
