package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import org.junit.jupiter.api.Test;

final class EchoScriptSleepCommandCompilerTest {
    @Test
    void sleepCommandsCompileAsLinearCommands() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile(
                "fixtures/sleep_opening.es", sleepOpeningSource());

        assertTrue(result.compilationSucceeded());
        assertSleepSequence(result.compiledStory().orElseThrow());
    }

    @Test
    void sleepBlinkCompilesAsRepeatedLinearCommands() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile(
                "fixtures/sleep_blink.es", sleepBlinkSource());

        assertTrue(result.compilationSucceeded());
        assertBlinkSequence(result.compiledStory().orElseThrow());
    }

    private static String sleepOpeningSource() {
        return """
                namespace JDH.Chapter1.Home;

                class SleepOpeningStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:first_night_dream", start: "start");
                    }

                    private void OnStart()
                    {
                        Story.Set("chapter_1.first_night_dream", true);
                        Sleep.CloseEyes(duration: Ticks(18));
                        Sleep.WaitUntilClosed();
                        Sleep.Wait(0.25);
                        Sleep.EnterDream("minecraft:overworld", 0.5, 64, 0.5, yaw: 180, pitch: 0);
                        Sleep.OpenEyes(duration: Ticks(16), sleepyBlink: true, showHud: false);
                        Sleep.WaitUntilOpen();
                        Sleep.ReturnToBed();
                        Sleep.CompleteNight();
                        Sleep.Complete();
                    }
                }
                """;
    }

    private static String sleepBlinkSource() {
        return """
                namespace JDH.Chapter1.Home;

                class SleepBlinkStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:sleep_blink", start: "start");
                    }

                    private void OnStart()
                    {
                        Sleep.Blink(
                            count: 2,
                            duration: Ticks(8),
                            closedWait: 0.12,
                            openWait: 0.18,
                            sleepyBlink: true,
                            showHud: false);
                        Sleep.Complete();
                    }
                }
                """;
    }

    private static void assertSleepSequence(StoryDefinition story) {
        StoryNode close = story.find(NodeId.of("close_eyes")).orElseThrow();
        StoryNode closed = story.find(NodeId.of("wait_until_closed")).orElseThrow();
        StoryNode wait = story.find(NodeId.of("wait_sleep")).orElseThrow();
        StoryNode dream = story.find(NodeId.of("enter_dream")).orElseThrow();
        StoryNode open = story.find(NodeId.of("open_eyes")).orElseThrow();
        StoryNode complete = story.find(NodeId.of("complete_sleep")).orElseThrow();

        assertEquals(NamespacedId.of("echo_player_sleep:close_eyes"), close.actionType());
        assertEquals("18", close.actionArguments().get("duration"));
        assertEquals(NodeId.of("wait_until_closed"), close.nextNode());
        assertEquals(NamespacedId.of("echo_player_sleep:wait_until_closed"), closed.actionType());
        assertEquals(NamespacedId.of("echo_player_sleep:wait"), wait.actionType());
        assertEquals("0.25", wait.actionArguments().get("seconds"));
        assertEquals(NamespacedId.of("echo_player_sleep:enter_dream"), dream.actionType());
        assertEquals("minecraft:overworld", dream.actionArguments().get("dimension"));
        assertEquals("0.5", dream.actionArguments().get("x"));
        assertEquals(NamespacedId.of("echo_player_sleep:open_eyes"), open.actionType());
        assertEquals("true", open.actionArguments().get("sleepyBlink"));
        assertEquals("false", open.actionArguments().get("showHud"));
        assertEquals(NodeId.of("finish"), complete.nextNode());
    }

    private static void assertBlinkSequence(StoryDefinition story) {
        StoryNode firstClose = story.find(NodeId.of("blink_close_eyes")).orElseThrow();
        StoryNode firstClosedWait = story.find(NodeId.of("blink_wait_closed")).orElseThrow();
        StoryNode firstOpen = story.find(NodeId.of("blink_open_eyes")).orElseThrow();
        StoryNode firstOpenWait = story.find(NodeId.of("blink_wait_open")).orElseThrow();
        StoryNode secondClose = story.find(NodeId.of("blink_close_eyes_2")).orElseThrow();
        StoryNode secondOpen = story.find(NodeId.of("blink_open_eyes_2")).orElseThrow();
        StoryNode secondOpenWait = story.find(NodeId.of("blink_wait_open_2")).orElseThrow();
        StoryNode complete = story.find(NodeId.of("complete_sleep")).orElseThrow();

        assertEquals(NamespacedId.of("echo_player_sleep:close_eyes"), firstClose.actionType());
        assertEquals("8", firstClose.actionArguments().get("duration"));
        assertEquals(NodeId.of("blink_wait_until_closed"), firstClose.nextNode());
        assertEquals(NamespacedId.of("echo_player_sleep:wait"), firstClosedWait.actionType());
        assertEquals("0.12", firstClosedWait.actionArguments().get("seconds"));
        assertEquals(NamespacedId.of("echo_player_sleep:open_eyes"), firstOpen.actionType());
        assertEquals("true", firstOpen.actionArguments().get("sleepyBlink"));
        assertEquals("false", firstOpen.actionArguments().get("showHud"));
        assertEquals(NodeId.of("blink_close_eyes_2"), firstOpenWait.nextNode());
        assertEquals(NamespacedId.of("echo_player_sleep:close_eyes"), secondClose.actionType());
        assertEquals(NamespacedId.of("echo_player_sleep:open_eyes"), secondOpen.actionType());
        assertEquals(NodeId.of("complete_sleep"), secondOpenWait.nextNode());
        assertEquals(NodeId.of("finish"), complete.nextNode());
    }
}
