package io.github.kxyr0.echonpcbackpack.script;

import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Normalized payload shared by Story, Dialogue and Trigger bridges. */
public record NpcBackpackScriptPayload(String actorId, String dimension, BlockPosition position) {
    private static final String DEFAULT_NAMESPACE = "jdh";
    private static final String OVERWORLD = "minecraft:overworld";

    public static NpcBackpackScriptPayload read(Map<String, String> values) {
        String actor = first(values, "npc", "actor", "actorId", "npcId", "arg0")
                .orElseThrow(() -> new IllegalArgumentException("NPC argument is missing."));
        Optional<Coordinates> encoded = first(values, "position", "block", "arg1")
                .flatMap(NpcBackpackScriptPayload::coordinates);
        int x = coordinate(values, encoded, "x", "arg1", Coordinates::x);
        int y = coordinate(values, encoded, "y", "arg2", Coordinates::y);
        int z = coordinate(values, encoded, "z", "arg3", Coordinates::z);
        String dimension = first(values, "dimension", "dim", "world", "worldId").orElse(OVERWORLD);
        return new NpcBackpackScriptPayload(namespaced(actor), dimension, new BlockPosition(x, y, z));
    }

    public String commandId(String owner, String operation) {
        String seed = owner + "|" + operation + "|" + actorId + "|" + dimension + "|" + position;
        String uuid = UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        return "echo_npc_sophisticated_backpacks:script_" + uuid;
    }

    private static int coordinate(Map<String, String> values, Optional<Coordinates> encoded,
            String named, String positional, java.util.function.ToIntFunction<Coordinates> read) {
        Optional<String> direct = first(values, named, positional);
        if (direct.isPresent() && coordinates(direct.orElseThrow()).isEmpty()) {
            return Integer.parseInt(direct.orElseThrow());
        }
        return encoded.map(read::applyAsInt)
                .orElseThrow(() -> new IllegalArgumentException(named + " argument is missing."));
    }

    private static Optional<Coordinates> coordinates(String value) {
        String text = value.trim();
        int open = text.indexOf('(');
        int close = text.lastIndexOf(')');
        if (open <= 0 || close != text.length() - 1) {
            return Optional.empty();
        }
        String type = text.substring(0, open);
        if (!type.equals("Position") && !type.equals("Vector3") && !type.equals("BlockPosition")) {
            return Optional.empty();
        }
        String[] parts = text.substring(open + 1, close).split(",", -1);
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            return Optional.of(new Coordinates(Integer.parseInt(parts[0].trim()),
                    Integer.parseInt(parts[1].trim()), Integer.parseInt(parts[2].trim())));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    private static String namespaced(String actor) {
        String value = actor.trim();
        return value.contains(":") ? value : DEFAULT_NAMESPACE + ":" + value;
    }

    private static Optional<String> first(Map<String, String> values, String... keys) {
        for (String key : keys) {
            String value = values.get(key);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private record Coordinates(int x, int y, int z) {
    }
}
