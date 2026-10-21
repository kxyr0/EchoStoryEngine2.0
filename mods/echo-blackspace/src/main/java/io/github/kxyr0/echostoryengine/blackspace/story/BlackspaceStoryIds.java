package io.github.kxyr0.echostoryengine.blackspace.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;

final class BlackspaceStoryIds {
    static final NamespacedId EXTENSION_ID = NamespacedId.of("echo_blackspace:story_bridge");
    static final NamespacedId CREATE = NamespacedId.of("echo_blackspace:create");
    static final NamespacedId TELEPORT_PLAYER = NamespacedId.of("echo_blackspace:teleport_player");
    static final String IMPLEMENTATION_VERSION = "0.1.0";

    private BlackspaceStoryIds() {
    }
}
