package io.github.kxyr0.echostoryengine.story.echoscript.environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentSourceInput;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.SetBlockChangeDef;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EchoScriptEnvironmentCompilerTest {
    private static final Instant NOW = Instant.parse("2026-07-31T08:00:00Z");

    @Test
    void validEnvironmentResourceCompiles() {
        EnvironmentCompilationResult result = compile(environmentSource());

        assertTrue(result.compilationSucceeded());
        assertTrue(result.diagnostics().isEmpty());
        EnvironmentResourceDef resource = result.environment().orElseThrow();
        SetBlockChangeDef breakfast = (SetBlockChangeDef) resource.states().get(1).changes().get(0);

        assertEquals(NamespacedId.of("jdh:home/kitchen_table"), resource.id());
        assertEquals(NamespacedId.of("minecraft:overworld"), resource.dimension());
        assertEquals(List.of("night", "morning_breakfast", "after_breakfast"),
                resource.states().stream().map(state -> state.id()).toList());
        assertEquals(NamespacedId.of("displaydelight:bacon_and_eggs"), breakfast.blockId());
        assertEquals("south", breakfast.properties().get(0).value());
    }

    @Test
    void duplicateStateIdIsRejected() {
        EnvironmentCompilationResult result = compile(environmentSource()
                .replace("State(\"morning_breakfast\", changes)", "State(\"night\", changes)"));

        assertFalse(result.compilationSucceeded());
        assertHasCode(result.diagnostics(), "ESE-ENV-SOURCE-002");
    }

    @Test
    void duplicatePositionChangeIsRejected() {
        EnvironmentCompilationResult result = compile(environmentSource()
                .replace("ClearBlock(BreadPosition)", "ClearBlock(SashaPlatePosition)"));

        assertFalse(result.compilationSucceeded());
        assertHasCode(result.diagnostics(), "ESE-ENV-SOURCE-003");
    }

    @Test
    void environmentSetCompilesAsStoryAction() {
        StoryCompilationResult result = new EchoScriptStorySourceCompiler().compile(new StorySourceInput(
                "sample:echo_story/stories/environment_set.es",
                """
                class EnvironmentStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:environment_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Environment.Set("jdh:home/kitchen_table", "morning_breakfast");
                    }
                }
                """), NOW);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode node = story.find(NodeId.of("set_environment")).orElseThrow();

        assertEquals(NamespacedId.of("echo_story_engine:environment_set"), node.actionType());
        assertEquals("jdh:home/kitchen_table", node.actionArguments().get("environmentId"));
        assertEquals("morning_breakfast", node.actionArguments().get("stateId"));
    }

    private EnvironmentCompilationResult compile(String source) {
        return new EchoScriptEnvironmentSourceCompiler().compile(
                new EnvironmentSourceInput("sample:echo_story/environments/kitchen_table.es", source), NOW);
    }

    private static void assertHasCode(List<StoryDiagnostic> diagnostics, String code) {
        assertTrue(diagnostics.stream().anyMatch(diagnostic -> diagnostic.code().equals(code)));
    }

    private static String environmentSource() {
        return environmentHeader()
                + nightState()
                + breakfastState()
                + afterBreakfastState()
                + sashaBreakfast()
                + "}\n";
    }

    private static String environmentHeader() {
        return """
                namespace JDH.Chapter1.Home;

                class KitchenTableEnvironment : EnvironmentResource
                {
                    private DimensionId World = "minecraft:overworld";
                    private Position SashaPlatePosition = Position(10, 64, -5);
                    private Position JohnPlatePosition = Position(11, 64, -5);
                    private Position BreadPosition = Position(12, 64, -5);
                    private EnvironmentStateDef[] states = [Night(), MorningBreakfast(), AfterBreakfast()];

                    public override EnvironmentResourceDef Build()
                    {
                        return Environment("jdh:home/kitchen_table", World, states);
                    }
                """;
    }

    private static String nightState() {
        return """
                    private EnvironmentStateDef Night()
                    {
                        var changes = [
                            ClearBlock(SashaPlatePosition),
                            ClearBlock(JohnPlatePosition),
                            ClearBlock(BreadPosition)
                        ];
                        return State("night", changes);
                    }
                """;
    }

    private static String breakfastState() {
        return """
                    private EnvironmentStateDef MorningBreakfast()
                    {
                        var changes = [
                            SashaBreakfast(),
                            SetBlock(JohnPlatePosition, "displaydelight:plated_bacon_sandwich"),
                            SetBlock(BreadPosition, "displaydelight:apple_cider")
                        ];
                        return State("morning_breakfast", changes);
                    }
                """;
    }

    private static String afterBreakfastState() {
        return """
                    private EnvironmentStateDef AfterBreakfast()
                    {
                        var changes = [
                            ClearBlock(SashaPlatePosition),
                            ClearBlock(JohnPlatePosition),
                            ClearBlock(BreadPosition)
                        ];
                        return State("after_breakfast", changes);
                    }
                """;
    }

    private static String sashaBreakfast() {
        return """
                    private EnvironmentChangeDef SashaBreakfast()
                    {
                        var properties = Properties(Property("facing", "south"));
                        return SetBlock(SashaPlatePosition, "displaydelight:bacon_and_eggs", properties);
                    }
                """;
    }
}
