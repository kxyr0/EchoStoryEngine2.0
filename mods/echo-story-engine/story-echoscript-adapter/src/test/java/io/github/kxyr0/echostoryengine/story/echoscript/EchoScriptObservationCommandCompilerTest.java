package io.github.kxyr0.echostoryengine.story.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NodeId;
import org.junit.jupiter.api.Test;

final class EchoScriptObservationCommandCompilerTest {
    @Test
    void observationCommandsCompileAsDurableExternalActions() {
        var result = EchoScriptAdapterFixture.compile("fixtures/observation_commands.es", """
                class ObservationStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("jdh:observation_story", start: "start");
                    }

                    private void OnStart()
                    {
                        Observation.Activate("jdh:museum_room");
                        Observation.Disable("jdh:museum_room#door_lock");
                        Observation.IsActive("jdh:museum_room");
                    }
                }
                """);

        assertTrue(result.compilationSucceeded(), () -> result.diagnostics().toString());
        var story = result.compiledStory().orElseThrow();
        var activate = story.find(NodeId.of("activate_observation")).orElseThrow();
        var disable = story.find(NodeId.of("disable_observation")).orElseThrow();
        var query = story.find(NodeId.of("is_observation_active")).orElseThrow();
        assertEquals("echo_observation:activate", activate.actionType().asString());
        assertEquals("jdh:museum_room", activate.actionArguments().get("target"));
        assertEquals(NodeId.of("disable_observation"), activate.nextNode());
        assertEquals("echo_observation:disable", disable.actionType().asString());
        assertEquals("echo_observation:is_active", query.actionType().asString());
        assertEquals("observation.isActive", query.outputName());
        assertEquals("observation.isActive", query.actionArguments().get("outputKey"));
    }
}
