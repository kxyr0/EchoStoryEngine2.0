package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import org.junit.jupiter.api.Test;

final class EchoScriptFeatureCommandCompilerTest {
    @Test
    void compactEs2CommandsKeepTheExternalActionContractAndOrderedTransitions() {
        var result = EchoScriptAdapterFixture.compile("jdh:echo_story/stories/unlock.es", """
                class UnlockStory : StoryResource
                {
                    OnStart()
                    {
                        Feature.Unlock("canvas");
                        Feature.Unlock("magic", player: "Alex");
                        Feature.Lock("magic");
                        Feature.Set("custom_feature", false);
                    }
                }
                """);
        assertTrue(result.compilationSucceeded(), () -> result.diagnostics().toString());
        var story = result.compiledStory().orElseThrow();
        var canvas = story.find(NodeId.of("set_feature")).orElseThrow();
        var magic = story.find(NodeId.of("set_feature_2")).orElseThrow();
        assertEquals("echo_core:set_flag", canvas.actionType().asString());
        assertEquals("thought_space_unlocked", canvas.actionArguments().get("flag"));
        assertEquals("true", canvas.actionArguments().get("value"));
        assertEquals("@a", canvas.actionArguments().get("player"));
        assertEquals(magic.id(), canvas.nextNode());
        assertEquals("magic_unlocked", magic.actionArguments().get("flag"));
        assertEquals("Alex", magic.actionArguments().get("player"));
        assertEquals("false", story.find(NodeId.of("set_feature_3")).orElseThrow().actionArguments().get("value"));
        var custom = story.find(NodeId.of("set_feature_4")).orElseThrow();
        assertEquals("custom_feature", custom.actionArguments().get("flag"));
        assertEquals(NodeId.of("finish"), custom.nextNode());
    }

    @Test
    void invalidArgumentsFailCompilation() {
        for (String command : new String[] {"Feature.Unlock();", "Feature.Unlock(42);",
                "Feature.Unlock(\"canvas\", next: \"manual\");", "Feature.Set(\"magic\", 7);",
                "Feature.Set(\"magic\");", "Feature.Unlock(\"../bad\");"}) {
            var result = EchoScriptAdapterFixture.compile("jdh:echo_story/stories/invalid.es",
                    "class InvalidStory : StoryResource { OnStart() { " + command + " } }");
            assertFalse(result.compilationSucceeded(), command);
        }
    }
}
