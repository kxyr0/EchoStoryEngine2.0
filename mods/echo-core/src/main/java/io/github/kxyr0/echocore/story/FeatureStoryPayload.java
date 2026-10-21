package io.github.kxyr0.echocore.story;

import io.github.kxyr0.echocore.features.FeatureState;
import java.util.Map;

public record FeatureStoryPayload(String player, String flag, boolean value) {
    public static FeatureStoryPayload parse(Map<String, String> payload) {
        String player = payload.get("player"), flag = payload.get("flag"), value = payload.get("value");
        if (player == null || player.isBlank()) throw new IllegalArgumentException("Feature action requires player (name, UUID, or @a)");
        if (!"true".equals(value) && !"false".equals(value)) throw new IllegalArgumentException("Feature action value must be true or false");
        return new FeatureStoryPayload(player, FeatureState.validate(flag), Boolean.parseBoolean(value));
    }
}
