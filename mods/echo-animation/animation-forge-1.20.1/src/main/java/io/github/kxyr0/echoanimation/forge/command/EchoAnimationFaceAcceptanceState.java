package io.github.kxyr0.echoanimation.forge.command;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationStateDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationTransitionDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.InterruptionPolicy;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.core.runtime.AnimationSubjectRuntime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

enum EchoAnimationFaceAcceptanceState {
    NEUTRAL("neutral", "face/neutral", "jdh:face_test_neutral", PlaybackMode.HOLD_LAST_FRAME),
    BLINK("blink", "face/blink", "jdh:face_test_blink", PlaybackMode.LOOP),
    BLINK_CLOSED("blink_closed", "face/blink_closed", "jdh:face_test_blink_closed",
            PlaybackMode.HOLD_LAST_FRAME),
    LOOK_LEFT("look_left", "face/look_left", "jdh:face_test_look_left", PlaybackMode.HOLD_LAST_FRAME),
    BROWS_RAISE("brows_raise", "face/brows_raise", "jdh:face_test_brows_raise", PlaybackMode.HOLD_LAST_FRAME),
    ANGRY_BROWS("angry_brows", "face/angry_brows", "jdh:face_test_angry_brows", PlaybackMode.HOLD_LAST_FRAME),
    FULL_FACE_HOLD("full_face_hold", "face/full_face_hold", "jdh:face_test_full_face_hold",
            PlaybackMode.HOLD_LAST_FRAME),
    FACE_NUDGE("face_nudge", "face/face_nudge", "jdh:face_test_face_nudge", PlaybackMode.LOOP);

    private final String wireName;
    private final AnimationStateId stateId;
    private final AnimationClipId clipId;
    private final PlaybackMode playbackMode;

    EchoAnimationFaceAcceptanceState(String wireName, String stateId, String clipId, PlaybackMode playbackMode) {
        this.wireName = wireName;
        this.stateId = new AnimationStateId(stateId);
        this.clipId = AnimationClipId.parse(clipId);
        this.playbackMode = playbackMode;
    }

    static Optional<EchoAnimationFaceAcceptanceState> from(String value) {
        return Arrays.stream(values()).filter(state -> state.wireName.equals(value)).findFirst();
    }

    static String availableNames() {
        return String.join(", ", Arrays.stream(values()).map(EchoAnimationFaceAcceptanceState::wireName).toList());
    }

    static List<AnimationStateDefinition> states() {
        AnimationChannelId face = new AnimationChannelId("face");
        return Arrays.stream(values()).map(state -> state.state(face)).toList();
    }

    static List<AnimationTransitionDefinition> transitions(AnimationChannelId channel, AnimationStateId neutral) {
        return Arrays.stream(values())
                .filter(state -> state != NEUTRAL)
                .map(state -> new AnimationTransitionDefinition(channel, neutral, state.wireName,
                        state.stateId, 0, 3L))
                .toList();
    }

    void enter(AnimationSubjectRuntime runtime) {
        if (this == NEUTRAL) {
            return;
        }
        var lease = runtime.acquire(new AnimationChannelId("face"),
                new AnimationOwnerId("echo_animation:face_acceptance")).lease();
        runtime.signal(lease.orElseThrow(), wireName);
    }

    AnimationClipId clipId() {
        return clipId;
    }

    String wireName() {
        return wireName;
    }

    private AnimationStateDefinition state(AnimationChannelId channel) {
        return new AnimationStateDefinition(stateId, channel, 40L, playbackMode,
                InterruptionPolicy.IMMEDIATE, Optional.of(clipId), 1.0D);
    }
}
