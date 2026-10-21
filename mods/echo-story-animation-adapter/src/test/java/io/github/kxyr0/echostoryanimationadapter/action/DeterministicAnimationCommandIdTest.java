package io.github.kxyr0.echostoryanimationadapter.action;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeIds;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import org.junit.jupiter.api.Test;

final class DeterministicAnimationCommandIdTest {
    @Test
    void mapsSameActionExecutionAndOperationToSameCommandId() {
        ActionExecutionId actionId = ActionExecutionId.of("sample:execution/wave");

        AnimationCommandId first = DeterministicAnimationCommandId.from(actionId,
                AnimationBridgeIds.PLAY_ANIMATION);
        AnimationCommandId second = DeterministicAnimationCommandId.from(actionId,
                AnimationBridgeIds.PLAY_ANIMATION);

        assertEquals(first, second);
        assertEquals("echo_story_animation:play_animation/sample/execution/wave", first.value());
    }

    @Test
    void operationTypeParticipatesInCommandId() {
        ActionExecutionId actionId = ActionExecutionId.of("sample:execution/wave");

        assertNotEquals(DeterministicAnimationCommandId.from(actionId, AnimationBridgeIds.PLAY_ANIMATION),
                DeterministicAnimationCommandId.from(actionId, AnimationBridgeIds.STOP_ANIMATION));
    }
}
