package io.github.kxyr0.echoanimation.echoscript.procedural.animation;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import java.util.Optional;

/**
 * Typed local values available inside a procedural AnimationResource Build method.
 */
sealed interface AnimationDslValue {
    record Channel(AnimationChannelId id) implements AnimationDslValue {
    }

    record State(AnimationChannelId channel, AnimationStateId id) implements AnimationDslValue {
    }

    record Clip(AnimationClipId id) implements AnimationDslValue {
    }

    record Play(AnimationClipId clip, Optional<String> segment, Optional<String> point)
            implements AnimationDslValue {
        Play(AnimationClipId clip) {
            this(clip, Optional.empty(), Optional.empty());
        }
    }
}
