package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class EchoScriptActionArgumentCompilerTest {
    @Test
    void actionArgumentsCompileFromEchoScriptNamedLiterals() {
        StoryDefinition story = compile(storyWithAction("""
                Action(
                    "confirm",
                    type: "echo_npc:spawn",
                    actorId: "echo_story_npc_test:john_father",
                    worldId: "minecraft:overworld",
                    x: "1.5",
                    y: 64,
                    present: true,
                    next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals(Map.of(
                "actorId", "echo_story_npc_test:john_father",
                "worldId", "minecraft:overworld",
                "x", "1.5",
                "y", "64",
                "present", "true"), action.actionArguments());
    }

    @Test
    void actionArgumentChangesStoryFingerprint() {
        StoryDefinition baseline = compile(EchoScriptAdapterFixture.minimalStory());
        StoryDefinition changedArgument = compile(storyWithAction(
                "Action(\"confirm\", type: \"sample:confirm\", actorId: \"sample:john\", next: \"finish\")"));

        assertFalse(fingerprint(baseline).equals(fingerprint(changedArgument)));
    }

    @Test
    void animationLifecycleActionArgumentsCompileThroughGenericActionSyntax() {
        StoryDefinition story = compile(storyWithAction("""
                Action(
                    "confirm",
                    type: "echo_animation:play_animation",
                    animation: "jdh:wave",
                    target: "jdh:john_father",
                    await: "STARTED",
                    next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals(Map.of(
                "animation", "jdh:wave",
                "target", "jdh:john_father",
                "await", "STARTED"), action.actionArguments());
    }

    @Test
    void storyLoadSugarCompilesToStoryStartAction() {
        StoryDefinition story = compile(storyWithAction(
                "Story.Load(\"confirm\", \"jdh:after_story\", "
                        + "executionId: \"jdh:after_execution\", next: \"finish\")"));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_story:start", action.actionType().asString());
        assertEquals(Map.of(
                "storyId", "jdh:after_story",
                "executionId", "jdh:after_execution"), action.actionArguments());
    }

    @Test
    void npcInteractSugarCompilesToInteractionAction() {
        StoryDefinition story = compile(storyWithAction(
                "Npc.Interact(\"confirm\", \"sample:father\", 100, 64, 100, next: \"finish\")"));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_npc_interaction:interact", action.actionType().asString());
        assertEquals(Map.of(
                "npc", "sample:father",
                "dimension", "minecraft:overworld",
                "x", "100",
                "y", "64",
                "z", "100",
                "face", "AUTO",
                "hand", "MAIN_HAND",
                "approach", "AUTO"), action.actionArguments());
    }

    @Test
    void npcStandUpSugarCompilesToInteractionAction() {
        StoryDefinition story = compile(storyWithAction(
                "Npc.StandUp(\"confirm\", \"sample:father\", next: \"finish\")"));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_npc_interaction:stand_up", action.actionType().asString());
        assertEquals(Map.of("npc", "sample:father"), action.actionArguments());
    }

    @Test
    void playerStandUpSugarCompilesToPlayerAction() {
        StoryDefinition story = compile(storyWithAction(
                "Player.StandUp(\"confirm\", next: \"finish\")"));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_player_actor:stand_up", action.actionType().asString());
        assertTrue(action.actionArguments().isEmpty());
    }

    @Test
    void sleepEnterDreamSugarCompilesToPlayerSleepAction() {
        StoryDefinition story = compile(storyWithAction("""
                Sleep.EnterDream(
                    "confirm",
                    "minecraft:overworld",
                    0.5,
                    64,
                    -12.5,
                    yaw: 180,
                    pitch: 0,
                    next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_player_sleep:enter_dream", action.actionType().asString());
        assertEquals(Map.of(
                "dimension", "minecraft:overworld",
                "x", "0.5",
                "y", "64",
                "z", "-12.5",
                "yaw", "180",
                "pitch", "0"), action.actionArguments());
    }

    @Test
    void sleepEyeSugarAcceptsTicksWrapper() {
        StoryDefinition story = compile(storyWithAction("""
                Sleep.OpenEyes(
                    "confirm",
                    duration: Ticks(16),
                    sleepyBlink: true,
                    showHud: false,
                    next: "finish")
                """));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_player_sleep:open_eyes", action.actionType().asString());
        assertEquals(Map.of("duration", "16", "sleepyBlink", "true", "showHud", "false"),
                action.actionArguments());
    }

    @Test
    void sleepWaitSugarUsesSeconds() {
        StoryDefinition story = compile(storyWithAction(
                "Sleep.Wait(\"confirm\", 0.25, next: \"finish\")"));

        StoryNode action = story.find(EchoScriptAdapterFixture.ACTION_NODE).orElseThrow();

        assertEquals("echo_player_sleep:wait", action.actionType().asString());
        assertEquals(Map.of("seconds", "0.25"), action.actionArguments());
    }

    @Test
    void nestedActionArgumentIsRejected() {
        StoryCompilationResult result = compileResult(storyWithAction(
                "Action(\"confirm\", type: \"sample:confirm\", actorId: Skin(\"sample:father\"), next: \"finish\")"));

        assertFalse(result.compilationSucceeded());
        assertTrue(result.compiledStory().isEmpty());
        assertTrue(result.diagnostics().stream().map(StoryDiagnostic::code).anyMatch("ESE-SOURCE-024"::equals));
    }

    private static StoryDefinition compile(String source) {
        return compileResult(source).compiledStory().orElseThrow();
    }

    private static StoryCompilationResult compileResult(String source) {
        return EchoScriptAdapterFixture.compile("fixtures/minimal_story.es", source);
    }

    private static String storyWithAction(String action) {
        return EchoScriptAdapterFixture.minimalStory().replace(
                "Action(\"confirm\", type: \"sample:confirm\", next: \"finish\")",
                action);
    }

    private static String fingerprint(StoryDefinition story) {
        return StoryDefinitionFingerprint.from(story).value();
    }
}
