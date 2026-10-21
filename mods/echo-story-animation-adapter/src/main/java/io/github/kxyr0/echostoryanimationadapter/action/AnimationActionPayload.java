package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal Story payload for semantic AnimationId actions.
 */
record AnimationActionPayload(
        AnimationId animationId,
        AnimationSubjectId subjectId,
        AwaitMode awaitMode
) {
    AnimationActionPayload {
        animationId = Objects.requireNonNull(animationId, "animationId");
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        awaitMode = Objects.requireNonNull(awaitMode, "awaitMode");
    }

    static AnimationActionPayload decode(Map<String, String> payload, boolean allowAwait) {
        return decode(payload, allowAwait, AwaitMode.STARTED);
    }

    static AnimationActionPayload decode(Map<String, String> payload, boolean allowAwait, AwaitMode defaultAwait) {
        Objects.requireNonNull(payload, "payload");
        AwaitMode await = allowAwait && payload.containsKey("await")
                ? AwaitMode.valueOf(required(payload, "await").toUpperCase(Locale.ROOT))
                : Objects.requireNonNull(defaultAwait, "defaultAwait");
        return new AnimationActionPayload(AnimationId.parse(required(payload, "animation")),
                new AnimationSubjectId(required(payload, "target")), await);
    }

    private static String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required animation action payload key: " + key);
        }
        return value.trim();
    }

    enum AwaitMode {
        STARTED,
        COMPLETED
    }
}
