package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import org.junit.jupiter.api.Test;

final class EchoScriptNpcMoveCompilerTest {
    @Test
    void graphNpcMoveDefaultsToStartedAwait() {
        StoryDefinition story = compile(minimalStoryWithAction("""
                Npc.Move("confirm", "sample:father", 200, 64, 180, next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_navigation:npc_move", action.actionType().asString());
        assertEquals("sample:father", action.actionArguments().get("npc"));
        assertEquals("STARTED", action.actionArguments().get("await"));
    }

    @Test
    void graphNpcMoveCanRequestArrivalAwait() {
        StoryDefinition story = compile(minimalStoryWithAction("""
                Npc.Move(
                    "confirm",
                    "sample:father",
                    200,
                    64,
                    180,
                    wait: true,
                    next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("ARRIVED", action.actionArguments().get("await"));
    }

    @Test
    void npcMoveCompilesAsLinearCommand() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/npc_move_story.es", """
                namespace JDH.Chapter1.Home;

                class MoveNpcStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:move_npc_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Npc.Move("jdh:sasha", 42, -3, 100);
                        Story.Set("chapter_1.npc_move_started", true);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode move = story.find(NodeId.of("move_npc")).orElseThrow();

        assertEquals(NamespacedId.of("echo_navigation:npc_move"), move.actionType());
        assertEquals("jdh:sasha", move.actionArguments().get("npc"));
        assertEquals("STARTED", move.actionArguments().get("await"));
        assertEquals(NodeId.of("set_variable"), move.nextNode());
    }

    @Test
    void npcSprintAcceptsVectorAndExplicitSpeed() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/npc_sprint_story.es", """
                namespace JDH.Chapter1.Home;

                class SprintNpcStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:sprint_npc_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Npc.Sprint("jdh:sasha", Vector3(42, -3, 100),
                            speed: 5.5, syncAnimation: false);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        StoryNode sprint = result.compiledStory().orElseThrow().find(NodeId.of("sprint_npc")).orElseThrow();
        assertEquals("SPRINT", sprint.actionArguments().get("gait"));
        assertEquals("42", sprint.actionArguments().get("x"));
        assertEquals("5.5", sprint.actionArguments().get("speed"));
        assertEquals("false", sprint.actionArguments().get("syncAnimation"));
    }

    private static StoryDefinition compile(String source) {
        return EchoScriptAdapterFixture.compile("fixtures/npc_move_story.es", source)
                .compiledStory()
                .orElseThrow();
    }

    private static String minimalStoryWithAction(String action) {
        return EchoScriptAdapterFixture.minimalStory().replace(
                "Action(\"confirm\", type: \"sample:confirm\", next: \"finish\")",
                action);
    }
}
