package io.github.kxyr0.echostoryanimationadapter.bridge;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import java.util.List;

/**
 * Stable public identifiers owned by the adapter.
 */
public final class AnimationBridgeIds {
    public static final String IMPLEMENTATION_VERSION = "0.1.0-SNAPSHOT";
    public static final NamespacedId EXTENSION_ID = NamespacedId.of("echo_story_animation_adapter:bridge");
    public static final NamespacedId PLAY_ANIMATION = NamespacedId.of("echo_animation:play_animation");
    public static final NamespacedId STOP_ANIMATION = NamespacedId.of("echo_animation:stop_animation");
    public static final NamespacedId WAIT_ANIMATION = NamespacedId.of("echo_animation:wait_animation");
    public static final List<NamespacedId> ACTION_TYPES = List.of(PLAY_ANIMATION, STOP_ANIMATION, WAIT_ANIMATION);

    private AnimationBridgeIds() {
    }
}
