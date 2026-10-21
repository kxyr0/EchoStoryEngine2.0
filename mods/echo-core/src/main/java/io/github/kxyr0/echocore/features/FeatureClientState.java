package io.github.kxyr0.echocore.features;

import java.util.Map;
import java.util.UUID;

public final class FeatureClientState {
    private static UUID owner;
    private static Map<String, Boolean> flags = Map.of();
    private FeatureClientState() { }
    public static void receive(UUID player, Map<String, Boolean> values) { owner = player; flags = Map.copyOf(values); }
    public static boolean allowed(UUID player, String flag) {
        return player != null && player.equals(owner) && flags.getOrDefault(FeatureState.validate(flag), false);
    }
    public static void clear() { owner = null; flags = Map.of(); }
}
