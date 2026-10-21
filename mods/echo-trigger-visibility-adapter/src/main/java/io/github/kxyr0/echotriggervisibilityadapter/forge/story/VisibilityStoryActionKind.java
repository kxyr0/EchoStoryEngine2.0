package io.github.kxyr0.echotriggervisibilityadapter.forge.story;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;

enum VisibilityStoryActionKind {
    ARM("arm"),
    DISARM("disarm"),
    WAIT_OUT("wait_until_out_of_view"),
    WAIT_VISIBLE("wait_until_visible"),
    IS_OUT("is_out_of_view"),
    IS_VISIBLE("is_visible"),
    ASSERT_OUT("assert_still_out_of_view");

    private final NamespacedId actionType;

    VisibilityStoryActionKind(String path) {
        actionType = NamespacedId.of("echo_visibility:" + path);
    }

    NamespacedId actionType() {
        return actionType;
    }

    boolean waiting() {
        return this == WAIT_OUT || this == WAIT_VISIBLE;
    }

    boolean desiredState(boolean visible, boolean outOfView) {
        return switch (this) {
            case WAIT_OUT, IS_OUT -> outOfView;
            case WAIT_VISIBLE, IS_VISIBLE -> visible;
            default -> false;
        };
    }
}
