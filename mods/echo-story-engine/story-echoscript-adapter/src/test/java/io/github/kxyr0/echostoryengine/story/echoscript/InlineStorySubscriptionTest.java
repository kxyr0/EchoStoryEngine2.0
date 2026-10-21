package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class InlineStorySubscriptionTest {
    @Test void nativeGraphContainsOnlyOnStartAndLeavesReactionToExternalEventBinder() {
        var result = EchoScriptAdapterFixture.compile("jdh:echo_story/stories/chapter.es", """
                class Chapter : StoryResource {
                    OnStart() { Story.Set("ready", true); }
                    When(Dialogue.Completed("jdh:father"), id: "after_father") {
                        Scene.Play("jdh:father_leaves");
                    }
                }
                """);
        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        var story = result.compiledStory().orElseThrow();
        assertEquals("jdh:chapter", story.storyId().asString());
        assertTrue(story.nodes().values().stream().noneMatch(node ->
                node.actionArguments().containsValue("jdh:father_leaves")));
    }
}
