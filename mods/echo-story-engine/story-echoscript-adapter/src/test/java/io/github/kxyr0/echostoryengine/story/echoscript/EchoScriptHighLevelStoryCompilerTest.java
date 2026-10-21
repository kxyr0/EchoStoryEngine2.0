package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import org.junit.jupiter.api.Test;

final class EchoScriptHighLevelStoryCompilerTest {
    @Test
    void infersIdentityAndStrictLinearGraphFromEs2Intent() {
        StoryDefinition story = compile("jdh:echo_story/stories/quests/find_notebook.es", """
                class FindNotebookQuest : StoryResource
                {
                    OnStart()
                    {
                        Story.Set("quests.find_notebook.accepted", true);
                        Dialogue.Load("jdh:find_notebook_accepted", output: "outcome");
                        Story.Require("outcome", "jdh:accepted");
                    }
                }
                """).compiledStory().orElseThrow();

        assertEquals(StoryId.of("jdh:find_notebook"), story.storyId());
        assertEquals(NodeId.of("start"), story.startNode());
        assertEquals("set_variable", story.find(NodeId.of("start")).orElseThrow().nextNode().value());
        assertEquals("jdh:find_notebook_accepted",
                story.find(NodeId.of("load_dialogue")).orElseThrow().actionArguments().get("dialogueId"));
        assertEquals("jdh:accepted",
                story.find(NodeId.of("require_variable")).orElseThrow().variableValue());
        assertTrue(story.find(NodeId.of("finish")).isPresent());
    }

    @Test
    void explicitIdAndGenericActionsPreserveStableExistingIdentity() {
        StoryDefinition story = compile("jdh:echo_story/stories/home_arrival.es", """
                class HomeArrivalQuest : StoryResource
                {
                    Id("jdh:chapter_1_home_arrival");

                    OnStart()
                    {
                        Action(
                            type: "echo_npc:spawn",
                            actorId: "jdh:john_father",
                            output: "result");
                    }
                }
                """).compiledStory().orElseThrow();

        assertEquals(StoryId.of("jdh:chapter_1_home_arrival"), story.storyId());
        assertEquals("echo_npc:spawn", story.find(NodeId.of("action")).orElseThrow().actionType().asString());
        assertEquals("jdh:john_father", story.find(NodeId.of("action")).orElseThrow()
                .actionArguments().get("actorId"));
        assertEquals("result", story.find(NodeId.of("action")).orElseThrow().outputName());
    }

    @Test
    void rejectsMixedLegacyAndEs2Profiles() {
        StoryCompilationResult result = compile("jdh:echo_story/stories/mixed.es", """
                class MixedQuest : StoryResource
                {
                    Id("jdh:mixed");
                    OnStart() { Story.Set("ready", true); }
                    private void Nodes() { Finish("finish"); }
                }
                """);

        assertFalse(result.compilationSucceeded());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ESE-SOURCE-019")));
    }

    @Test
    void requiresCanonicalSourcePathWhenIdIsInferred() {
        StoryCompilationResult result = compile("inline.es", """
                class InlineQuest : StoryResource
                {
                    OnStart() { Story.Set("ready", true); }
                }
                """);

        assertFalse(result.compilationSucceeded());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ESE-SOURCE-004")));
    }

    private static StoryCompilationResult compile(String sourceId, String source) {
        return EchoScriptAdapterFixture.compile(sourceId, source);
    }
}
