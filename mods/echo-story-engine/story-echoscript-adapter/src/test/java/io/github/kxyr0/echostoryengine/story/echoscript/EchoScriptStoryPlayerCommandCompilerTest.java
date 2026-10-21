package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import org.junit.jupiter.api.Test;

final class EchoScriptStoryPlayerCommandCompilerTest {
    @Test
    void openingCommandsCompileAsLinearCommands() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/player_opening.es", """
                namespace JDH.Chapter1.Home;

                class Chapter1OpeningStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:chapter_1_opening", start: "start");
                    }

                    private void OnStart()
                    {
                        Story.Set("chapter_1.opening_started", true);
                        Screen.SetBlack(0, 1);
                        Npc.SpawnAt("jdh:sasha", 48, -2, 92, yaw: 180, pitch: 0);
                        Npc.SleepAt("jdh:sasha", 49, -3, 92);
                        Npc.WakeUp("jdh:sasha");
                        Player.SpawnAt(48, -2, 90, yaw: 180, pitch: 8);
                        Player.SetHotbarItem(1, "minecraft:book", count: 1);
                        Player.SelectHotbarSlot(1);
                        Player.SitAt(48, -2, 90, timeoutTicks: 40);
                        Dialogue.Load("jdh:start_story", output: "outcome");
                        Story.Set("chapter_1.opening_completed", true);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        assertNpcSequence(story);
        assertPlayerSequence(story);
    }

    @Test
    void blackspaceCreateAndTeleportCompileAsLinearCommands() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/blackspace_opening.es", """
                namespace JDH.Chapter1.Home;

                class BlackspaceOpeningStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:blackspace_opening", start: "start");
                    }

                    private void OnStart()
                    {
                        var blackspace = await Blackspace.Create("jdh:first_contact");
                        Player.Teleport(blackspace.Spawn);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode create = story.find(NodeId.of("create_blackspace")).orElseThrow();
        StoryNode teleport = story.find(NodeId.of("teleport_player")).orElseThrow();

        assertEquals(NamespacedId.of("echo_blackspace:create"), create.actionType());
        assertEquals("jdh:first_contact", create.actionArguments().get("instanceId"));
        assertEquals("blackspace.Spawn", create.actionArguments().get("spawnOutput"));
        assertEquals("blackspace.Spawn", create.outputName());
        assertEquals(NodeId.of("teleport_player"), create.nextNode());
        assertEquals(NamespacedId.of("echo_blackspace:teleport_player"), teleport.actionType());
        assertEquals("jdh:first_contact", teleport.actionArguments().get("instanceId"));
    }

    @Test
    void standUpCommandsCompileAsLinearCommands() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/stand_up_story.es", """
                namespace JDH.Chapter1.Home;

                class StandUpStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:stand_up_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Npc.Interact("jdh:sasha", 42, -3, 100);
                        Npc.StandUp("jdh:sasha");
                        Player.SitAt(48, -2, 90);
                        Player.StandUp();
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode interact = story.find(NodeId.of("interact_npc")).orElseThrow();
        StoryNode npcStandUp = story.find(NodeId.of("stand_up_npc")).orElseThrow();
        StoryNode sit = story.find(NodeId.of("sit_player")).orElseThrow();
        StoryNode playerStandUp = story.find(NodeId.of("stand_up_player")).orElseThrow();

        assertEquals(NamespacedId.of("echo_npc_interaction:interact"), interact.actionType());
        assertEquals(NodeId.of("stand_up_npc"), interact.nextNode());
        assertEquals(NamespacedId.of("echo_npc_interaction:stand_up"), npcStandUp.actionType());
        assertEquals("jdh:sasha", npcStandUp.actionArguments().get("npc"));
        assertEquals(NodeId.of("sit_player"), npcStandUp.nextNode());
        assertEquals(NamespacedId.of("echo_player_actor:sit_at"), sit.actionType());
        assertEquals(NodeId.of("stand_up_player"), sit.nextNode());
        assertEquals(NamespacedId.of("echo_player_actor:stand_up"), playerStandUp.actionType());
        assertTrue(playerStandUp.actionArguments().isEmpty());
    }

    @Test
    void storyLoadCompilesAsLinearCommand() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/story_load.es", """
                namespace JDH.Chapter1.Home;

                class StoryChainStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:story_chain", start: "start");
                    }

                    private void OnStart()
                    {
                        Story.Load("jdh:after_story");
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode load = story.find(NodeId.of("load_story")).orElseThrow();

        assertEquals(NamespacedId.of("echo_story:start"), load.actionType());
        assertEquals("jdh:after_story", load.actionArguments().get("storyId"));
    }

    private static void assertNpcSequence(StoryDefinition story) {
        StoryNode start = story.find(NodeId.of("start")).orElseThrow();
        StoryNode opened = story.find(NodeId.of("set_variable")).orElseThrow();
        StoryNode setBlack = story.find(NodeId.of("set_black")).orElseThrow();
        StoryNode npcSpawn = story.find(NodeId.of("spawn_npc")).orElseThrow();
        StoryNode npcSleep = story.find(NodeId.of("sleep_npc")).orElseThrow();
        StoryNode npcWake = story.find(NodeId.of("wake_npc")).orElseThrow();

        assertEquals(NodeId.of("set_variable"), start.nextNode());
        assertEquals(NodeId.of("set_black"), opened.nextNode());
        assertEquals(NamespacedId.of("echo_screen:set_black"), setBlack.actionType());
        assertEquals("0", setBlack.actionArguments().get("canvasGroup"));
        assertEquals("1", setBlack.actionArguments().get("time"));
        assertEquals(NodeId.of("spawn_npc"), setBlack.nextNode());
        assertEquals(NamespacedId.of("echo_npc:spawn_at"), npcSpawn.actionType());
        assertEquals("jdh:sasha", npcSpawn.actionArguments().get("npcId"));
        assertEquals("-2", npcSpawn.actionArguments().get("y"));
        assertEquals("minecraft:overworld", npcSpawn.actionArguments().get("worldId"));
        assertEquals(NodeId.of("sleep_npc"), npcSpawn.nextNode());
        assertEquals(NamespacedId.of("echo_npc:sleep_at"), npcSleep.actionType());
        assertEquals("-3", npcSleep.actionArguments().get("bedY"));
        assertFalse(npcSleep.actionArguments().containsKey("expectedBlock"));
        assertEquals(NodeId.of("wake_npc"), npcSleep.nextNode());
        assertEquals(NamespacedId.of("echo_npc:wake_up"), npcWake.actionType());
        assertEquals("jdh:sasha", npcWake.actionArguments().get("npcId"));
        assertEquals(NodeId.of("spawn_player"), npcWake.nextNode());
    }

    private static void assertPlayerSequence(StoryDefinition story) {
        StoryNode spawn = story.find(NodeId.of("spawn_player")).orElseThrow();
        StoryNode item = story.find(NodeId.of("set_hotbar_item")).orElseThrow();
        StoryNode dialogue = story.find(NodeId.of("load_dialogue")).orElseThrow();

        assertEquals(NamespacedId.of("echo_player_actor:spawn_at"), spawn.actionType());
        assertEquals("48", spawn.actionArguments().get("x"));
        assertEquals("8", spawn.actionArguments().get("pitch"));
        assertEquals(NodeId.of("set_hotbar_item"), spawn.nextNode());
        assertEquals(NamespacedId.of("echo_player_actor:set_hotbar_item"), item.actionType());
        assertEquals("minecraft:book", item.actionArguments().get("itemId"));
        assertEquals(NamespacedId.of("echo_dialogue:play"), dialogue.actionType());
        assertEquals("jdh:start_story", dialogue.actionArguments().get("dialogueId"));
        assertFalse(dialogue.actionArguments().containsKey("audienceId"));
        assertEquals("outcome", dialogue.outputName());
    }
}
