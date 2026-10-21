package io.github.kxyr0.echostoryanimationadapter.action;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationCompletionMapping.Scope;
import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeIds;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionSink;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

final class AnimationCompletionMappingTest {
    private static final AnimationId ANIMATION = AnimationId.parse("jdh:wave");
    private static final AnimationSubjectId SUBJECT = new AnimationSubjectId("jdh:john_father");
    private static final AnimationActionPayload PAYLOAD = new AnimationActionPayload(
            ANIMATION, SUBJECT, AnimationActionPayload.AwaitMode.COMPLETED);
    private static final Scope SCOPE = new Scope(StoryId.of("jdh:story"), ExecutionId.of("jdh:run"),
            ActionExecutionId.of("jdh:run/wait"), AnimationBridgeIds.WAIT_ANIMATION);

    @Test
    void terminalQueryMapsToStableSuccessOutputs() {
        RecordingSink sink = new RecordingSink();
        AnimationPlaybackQueryResult terminal = new AnimationPlaybackQueryResult(
                AnimationPlaybackQueryResult.Status.TERMINAL,
                ANIMATION,
                SUBJECT,
                Optional.of(AnimationPlaybackId.parse("ptest")),
                Optional.empty(),
                Optional.of(AnimationPlaybackOutcome.COMPLETED),
                7L,
                4L,
                "EANIM-A5-TERMINAL",
                "terminal");

        AnimationCompletionMapping.terminal(SCOPE, PAYLOAD, terminal, sink);

        ActionCompletion completion = sink.completion();
        assertEquals(ExternalActionResultCategory.SUCCESS, completion.resultCategory());
        assertEquals("COMPLETED", completion.outputs().get("outcome"));
        assertEquals("ptest", completion.outputs().get("animationPlaybackId"));
        assertEquals("TERMINAL", completion.outputs().get("phase"));
        assertEquals("7", completion.outputs().get("subjectRevision"));
        assertEquals("4", completion.outputs().get("playbackRevision"));
    }

    @Test
    void activeQueryCanExposePhaseForStartedCompletion() {
        AnimationPlaybackQueryResult active = new AnimationPlaybackQueryResult(
                AnimationPlaybackQueryResult.Status.ACTIVE,
                ANIMATION,
                SUBJECT,
                Optional.of(AnimationPlaybackId.parse("pactive")),
                Optional.of(AnimationPhase.ENTER),
                Optional.empty(),
                3L,
                1L,
                "EANIM-A5-ACTIVE",
                "active");

        assertEquals(AnimationPhase.ENTER, active.phase().orElseThrow());
    }

    private static final class RecordingSink implements ActionCompletionSink {
        private final AtomicReference<ActionCompletion> completion = new AtomicReference<>();

        @Override
        public ActionCompletionAcceptance complete(ActionCompletion accepted) {
            completion.set(accepted);
            return ActionCompletionAcceptance.ACCEPTED;
        }

        ActionCompletion completion() {
            return completion.get();
        }
    }
}
