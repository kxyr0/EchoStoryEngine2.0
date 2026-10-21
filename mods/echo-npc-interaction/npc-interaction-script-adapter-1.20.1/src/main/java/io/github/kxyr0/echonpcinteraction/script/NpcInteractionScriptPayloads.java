package io.github.kxyr0.echonpcinteraction.script;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Map;
import java.util.Optional;

public final class NpcInteractionScriptPayloads {
    private static final String JDH_DEFAULT_NAMESPACE = "jdh";

    private NpcInteractionScriptPayloads() {
    }

    public static InteractionCommand interact(Map<String, String> arguments) {
        return new InteractionCommand(actor(arguments), position(arguments),
                first(arguments, "dimension", "dim", "world", "worldId").orElse("minecraft:overworld"),
                first(arguments, "face").orElse("AUTO"), first(arguments, "hand").orElse("MAIN_HAND"),
                first(arguments, "approach").orElse("AUTO"));
    }

    public static ActorId actor(Map<String, String> arguments) {
        String raw = first(arguments, "npc", "actor", "actorId", "npcId", "target", "arg0")
                .orElseThrow(() -> new IllegalArgumentException("NPC argument is missing."));
        return new ActorId(normalizedActorId(raw));
    }

    private static String normalizedActorId(String raw) {
        String value = raw.trim();
        return value.contains(":") ? value : JDH_DEFAULT_NAMESPACE + ":" + value;
    }

    private static BlockPosition position(Map<String, String> arguments) {
        return new BlockPosition(intValue(arguments, "x", "arg1"),
                intValue(arguments, "y", "arg2"), intValue(arguments, "z", "arg3"));
    }

    private static int intValue(Map<String, String> arguments, String named, String positional) {
        return Integer.parseInt(first(arguments, named, positional)
                .orElseThrow(() -> new IllegalArgumentException(named + " argument is missing.")));
    }

    private static Optional<String> first(Map<String, String> arguments, String... keys) {
        for (String key : keys) {
            String value = arguments.get(key);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public record InteractionCommand(
            ActorId actorId,
            BlockPosition position,
            String dimension,
            String face,
            String hand,
            String approach) {
    }
}
