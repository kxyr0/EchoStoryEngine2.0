package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.execution.ActionStartRequest;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class VisibilityStoryRequestContextTest {
    private static final UUID PLAYER = UUID.fromString("30da95c2-5828-44b7-a73d-0e84bc36545e");

    @Test
    void explicitPlayerPayloadSurvivesDurableActionRecovery() {
        var context = VisibilityStoryRequestContext.resolve(request(
                "sample:ordinary_execution", Map.of(
                        "player", PLAYER.toString(),
                        "watchId", "jdh:father_vanish"))).orElseThrow();

        assertEquals(PLAYER, context.playerId());
        assertEquals("jdh:father_vanish", context.watchId().value());
    }

    @Test
    void visibilityExecutionIdentityRecoversPlayerWithoutTransientBridge() {
        var context = VisibilityStoryRequestContext.resolve(request(
                "echo_visibility:story/watch/generation/2/player/" + PLAYER + "/lost_sight/4",
                Map.of("watchId", "jdh:father_vanish"))).orElseThrow();

        assertEquals(PLAYER, context.playerId());
    }

    @Test
    void missingPlayerIdentityIsRejected() {
        assertTrue(VisibilityStoryRequestContext.resolve(request(
                "sample:ordinary_execution", Map.of("watchId", "jdh:father_vanish"))).isEmpty());
    }

    private static ActionStartRequest request(String executionId, Map<String, String> payload) {
        return new ActionStartRequest(StoryId.of("sample:story"), ExecutionId.of(executionId),
                ActionExecutionId.of("sample:action"), NamespacedId.of("echo_visibility:arm"), payload);
    }
}
