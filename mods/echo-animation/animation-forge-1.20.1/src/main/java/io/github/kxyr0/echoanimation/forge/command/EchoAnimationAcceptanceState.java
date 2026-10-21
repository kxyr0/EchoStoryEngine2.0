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

enum EchoAnimationAcceptanceState {
    IDLE("idle", "body/idle", "jdh:humanoid_idle"),
    WAVE("wave", "body/wave", "jdh:humanoid_test_wave"),
    KNEE("knee", "body/knee", "jdh:humanoid_test_knee"),
    HEAD("head", "body/head", "jdh:humanoid_test_head"),
    BODY("body", "body/body", "jdh:humanoid_test_body"),
    LEFT_ARM("left_arm", "body/left_arm", "jdh:humanoid_test_left_arm"),
    RIGHT_ARM("right_arm", "body/right_arm", "jdh:humanoid_test_right_arm"),
    BOTH_ARMS("both_arms", "body/both_arms", "jdh:humanoid_test_both_arms"),
    LEFT_LEG("left_leg", "body/left_leg", "jdh:humanoid_test_left_leg"),
    RIGHT_LEG("right_leg", "body/right_leg", "jdh:humanoid_test_right_leg"),
    STEP("step", "body/step", "jdh:humanoid_test_step"),
    ACTING("acting", "body/acting", "jdh:humanoid_test_acting", 80L);

    private final String wireName;
    private final AnimationStateId stateId;
    private final AnimationClipId clipId;
    private final long durationTicks;

    EchoAnimationAcceptanceState(String wireName, String stateId, String clipId) {
        this(wireName, stateId, clipId, 60L);
    }

    EchoAnimationAcceptanceState(String wireName, String stateId, String clipId, long durationTicks) {
        this.wireName = wireName;
        this.stateId = new AnimationStateId(stateId);
        this.clipId = AnimationClipId.parse(clipId);
        this.durationTicks = durationTicks;
    }

    static Optional<EchoAnimationAcceptanceState> from(String value) {
        return Arrays.stream(values()).filter(state -> state.wireName.equals(value)).findFirst();
    }

    static String availableNames() {
        return String.join(", ", Arrays.stream(values()).map(EchoAnimationAcceptanceState::wireName).toList());
    }

    static List<AnimationStateDefinition> states() {
        AnimationChannelId body = new AnimationChannelId("body");
        return Arrays.stream(values()).map(state -> state.state(body)).toList();
    }

    static List<AnimationTransitionDefinition> transitions(
            AnimationChannelId channelId,
            AnimationStateId idleStateId,
            long blendTicks
    ) {
        return Arrays.stream(values())
                .filter(state -> state != IDLE)
                .map(state -> new AnimationTransitionDefinition(channelId, idleStateId, state.wireName,
                        state.stateId, 0, blendTicks))
                .toList();
    }

    void enter(AnimationSubjectRuntime runtime) {
        if (this == IDLE) {
            return;
        }
        var lease = runtime.acquire(new AnimationChannelId("body"),
                new AnimationOwnerId("echo_animation:acceptance")).lease();
        runtime.signal(lease.orElseThrow(), wireName);
    }

    AnimationClipId clipId() {
        return clipId;
    }

    String wireName() {
        return wireName;
    }

    private AnimationStateDefinition state(AnimationChannelId channelId) {
        return new AnimationStateDefinition(stateId, channelId, durationTicks, PlaybackMode.LOOP,
                InterruptionPolicy.IMMEDIATE, Optional.of(clipId), 1.0D);
    }
}
