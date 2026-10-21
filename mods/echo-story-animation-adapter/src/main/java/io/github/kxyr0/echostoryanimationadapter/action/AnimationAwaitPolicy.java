package io.github.kxyr0.echostoryanimationadapter.action;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echostoryanimationadapter.action.AnimationActionPayload.AwaitMode;
import io.github.kxyr0.echostoryanimationadapter.bridge.AnimationBridgeIds;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.extension.api.outcome.ActionInvocationResult;
import java.util.Map;
import java.util.Optional;

/**
 * Applies public AnimationDefinition main-mode rules to Story await behavior.
 */
final class AnimationAwaitPolicy {
    private AnimationAwaitPolicy() {
    }

    static AwaitMode defaultAwait(
            NamespacedId actionType,
            AnimationService service,
            Map<String, String> rawPayload
    ) {
        if (!AnimationBridgeIds.PLAY_ANIMATION.equals(actionType)) {
            return AwaitMode.STARTED;
        }
        return mainMode(service, rawPayload)
                .filter(MainMode.ONCE::equals)
                .map(mode -> AwaitMode.COMPLETED)
                .orElse(AwaitMode.STARTED);
    }

    static Optional<ActionInvocationResult> rejectImpossibleAwait(
            NamespacedId actionType,
            AnimationService service,
            Map<String, String> rawPayload
    ) {
        if (!AnimationBridgeIds.PLAY_ANIMATION.equals(actionType)
                || !"COMPLETED".equalsIgnoreCase(rawPayload.getOrDefault("await", "").trim())) {
            return Optional.empty();
        }
        boolean impossible = mainMode(service, rawPayload)
                .filter(mode -> mode != MainMode.ONCE)
                .isPresent();
        return impossible ? Optional.of(ActionInvocationResult.rejected(
                "await COMPLETED is only allowed for ONCE animations.")) : Optional.empty();
    }

    private static Optional<MainMode> mainMode(AnimationService service, Map<String, String> rawPayload) {
        String rawAnimation = rawPayload.get("animation");
        return rawAnimation == null ? Optional.empty() : service.animationMainMode(AnimationId.parse(rawAnimation));
    }
}
