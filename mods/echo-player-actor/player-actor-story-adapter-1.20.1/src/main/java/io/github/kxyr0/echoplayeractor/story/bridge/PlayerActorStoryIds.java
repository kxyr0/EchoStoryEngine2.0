package io.github.kxyr0.echoplayeractor.story.bridge;

import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorCommandId;
import io.github.kxyr0.echoplayeractor.api.identity.PlayerActorOwnerId;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;

final class PlayerActorStoryIds {
    static final String IMPLEMENTATION_VERSION = "0.2.0";
    static final NamespacedId EXTENSION_ID = new NamespacedId("echo_story_player_actor_adapter", "bridge");
    static final NamespacedId SPAWN_AT = new NamespacedId("echo_player_actor", "spawn_at");
    static final NamespacedId SET_HOTBAR_ITEM = new NamespacedId("echo_player_actor", "set_hotbar_item");
    static final NamespacedId SELECT_HOTBAR_SLOT = new NamespacedId("echo_player_actor", "select_hotbar_slot");
    static final NamespacedId SIT_AT = new NamespacedId("echo_player_actor", "sit_at");

    private PlayerActorStoryIds() {
    }

    static PlayerActorOwnerId owner(ActionExecutionId actionId) {
        return PlayerActorOwnerId.system("story/" + actionId.asString());
    }

    static PlayerActorCommandId commandId(ActionExecutionId actionId) {
        return new PlayerActorCommandId("story-action/" + actionId.asString());
    }
}
