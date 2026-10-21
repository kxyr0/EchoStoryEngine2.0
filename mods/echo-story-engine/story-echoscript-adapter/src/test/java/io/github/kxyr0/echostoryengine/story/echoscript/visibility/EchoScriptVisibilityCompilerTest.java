package io.github.kxyr0.echostoryengine.story.echoscript.visibility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNode;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class EchoScriptVisibilityCompilerTest {
    @Test
    void allViewActionsCompileInLinearStoryOrder() {
        var input = new StorySourceInput("fixtures/visibility_story.es", """
                namespace JDH.Visibility;

                class VisibilityStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:visibility_story", start: "start");
                    }

                    private void OnStart()
                    {
                        View.Arm("jdh:father_vanish");
                        View.WaitUntilOutOfView("jdh:father_vanish");
                        View.IsOutOfView("jdh:father_vanish");
                        View.WaitUntilVisible("jdh:father_vanish");
                        View.IsVisible("jdh:father_vanish");
                        View.AssertStillOutOfView("jdh:father_vanish");
                        View.Disarm("jdh:father_vanish");
                    }
                }
                """);
        var result = new EchoScriptStorySourceCompiler().compile(
                input, Instant.parse("2026-08-02T00:00:00Z"));

        assertTrue(result.compilationSucceeded(), result.diagnostics().toString());
        StoryDefinition story = result.compiledStory().orElseThrow();
        List<String> expected = List.of("arm", "wait_until_out_of_view", "is_out_of_view",
                "wait_until_visible", "is_visible", "assert_still_out_of_view", "disarm");
        NodeId current = story.find(story.startNode()).orElseThrow().nextNode();
        for (String actionName : expected) {
            StoryNode action = story.find(current).orElseThrow();
            assertEquals("echo_visibility:" + actionName, action.actionType().asString());
            assertEquals("jdh:father_vanish", action.actionArguments().get("watchId"));
            current = action.nextNode();
        }
    }
}
