package io.github.kxyr0.echocore.features;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class FeatureState {
    private final Map<UUID, Map<String, Boolean>> players = new HashMap<>();
    public static String validate(String flag) {
        if (flag == null || !flag.matches("[a-z][a-z0-9_]{0,63}")) throw new IllegalArgumentException("Invalid feature flag: " + flag);
        return flag;
    }
    public boolean allowed(UUID player, String flag) { return players.getOrDefault(player, Map.of()).getOrDefault(validate(flag), false); }
    public void set(UUID player, String flag, boolean value) {
        var flags = players.computeIfAbsent(player, unused -> new HashMap<>());
        String key = validate(flag);
        if (!flags.containsKey(key) && flags.size() >= 256) throw new IllegalArgumentException("Too many feature flags for player");
        flags.put(key, value);
    }
    public Map<String, Boolean> snapshot(UUID player) { return Map.copyOf(players.getOrDefault(player, Map.of())); }
    public Map<UUID, Map<String, Boolean>> snapshot() {
        Map<UUID, Map<String, Boolean>> copy = new HashMap<>();
        players.forEach((id, flags) -> copy.put(id, Map.copyOf(flags)));
        return Map.copyOf(copy);
    }
}
