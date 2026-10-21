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

final class EchoScriptStoryBlackspaceSleepCompilerTest {
    @Test
    void blackspaceSpawnCompilesAsSleepDreamTarget() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/blackspace_sleep.es", """
                namespace JDH.Chapter1.Home;

                class BlackspaceSleepStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:blackspace_sleep", start: "start");
                    }

                    private void OnStart()
                    {
                        var blackspace = await Blackspace.Create("jdh:first_contact");
                        Sleep.EnterDream(blackspace.Spawn);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode create = story.find(NodeId.of("create_blackspace")).orElseThrow();
        StoryNode dream = story.find(NodeId.of("enter_dream")).orElseThrow();

        assertEquals("blackspace.Spawn", create.outputName());
        assertEquals("blackspace.Spawn", create.actionArguments().get("spawnOutput"));
        assertEquals(NamespacedId.of("echo_player_sleep:enter_dream"), dream.actionType());
        assertEquals("blackspace.Spawn", dream.actionArguments().get("target"));
    }

    @Test
    void blackspaceInstanceCompilesAsSleepLifecycleTarget() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/blackspace_sleep_lifecycle.es", """
                namespace JDH.Chapter1.Home;

                class BlackspaceSleepStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:blackspace_sleep", start: "start");
                    }

                    private void OnStart()
                    {
                        var blackspace = await Blackspace.Create("jdh:first_contact");
                        Sleep.CloseEyes(duration: Ticks(24));
                        Sleep.WaitUntilClosed();
                        Sleep.EnterDream(blackspace);
                        Sleep.OpenEyes(duration: Ticks(12), sleepyBlink: true);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode dream = story.find(NodeId.of("enter_dream")).orElseThrow();

        assertEquals(NamespacedId.of("echo_player_sleep:enter_dream"), dream.actionType());
        assertEquals("jdh:first_contact", dream.actionArguments().get("blackspaceInstanceId"));
        assertFalse(dream.actionArguments().containsKey("target"));
    }

    @Test
    void blackspaceCreateWithoutAwaitCompilesAsSleepDreamTarget() {
        StoryCompilationResult result = EchoScriptAdapterFixture.compile("fixtures/blackspace_sleep_no_await.es", """
                namespace JDH.Chapter1.Home;

                class BlackspaceSleepStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:blackspace_sleep", start: "start");
                    }

                    private void OnStart()
                    {
                        var blackspace = Blackspace.Create("jdh:first_contact");
                        Sleep.EnterDream(blackspace.Spawn);
                    }
                }
                """);

        assertTrue(result.compilationSucceeded());
        StoryDefinition story = result.compiledStory().orElseThrow();
        StoryNode create = story.find(NodeId.of("create_blackspace")).orElseThrow();
        StoryNode dream = story.find(NodeId.of("enter_dream")).orElseThrow();

        assertEquals("blackspace.Spawn", create.outputName());
        assertEquals("blackspace.Spawn", create.actionArguments().get("spawnOutput"));
        assertEquals(NamespacedId.of("echo_player_sleep:enter_dream"), dream.actionType());
        assertEquals("blackspace.Spawn", dream.actionArguments().get("target"));
    }
}
