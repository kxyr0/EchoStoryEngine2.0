package io.github.kxyr0.echostorynpcadapter.action;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Map;
import java.util.Set;

/**
 * Decodes Story string payloads into Echo NPC public value objects.
 */
public final class ActionPayloadDecoder {
    private static final Set<String> SPAWN = Set.of("actorId", "definitionId", "worldId", "x", "y", "z",
            "yaw", "pitch");
    private static final Set<String> SPAWN_AT = Set.of("npcId", "worldId", "x", "y", "z", "yaw", "pitch");
    private static final Set<String> TELEPORT = Set.of("actorId", "worldId", "x", "y", "z", "yaw", "pitch");
    private static final Set<String> DESPAWN = Set.of("actorId");
    private static final Set<String> SLEEP_AT = Set.of("npcId", "worldId", "bedX", "bedY", "bedZ",
            "expectedBlock");
    private static final Set<String> SLEEP_AT_REQUIRED = Set.of("npcId", "worldId", "bedX", "bedY", "bedZ");
    private static final String ANY_BED = "echo_npc:any_bed";
    private static final Set<String> WAKE_UP = Set.of("npcId");

    public SpawnPayload spawn(Map<String, String> payload) {
        requireExact(payload, SPAWN, "spawn");
        return new SpawnPayload(
                new ActorId(required(payload, "actorId")),
                new NpcDefinitionId(required(payload, "definitionId")),
                new NpcWorldId(required(payload, "worldId")),
                position(payload),
                rotation(payload));
    }

    public SpawnPayload spawnAt(Map<String, String> payload) {
        requireExact(payload, SPAWN_AT, "spawn_at");
        String npcId = required(payload, "npcId");
        return new SpawnPayload(
                new ActorId(npcId),
                new NpcDefinitionId(npcId),
                new NpcWorldId(required(payload, "worldId")),
                position(payload),
                rotation(payload));
    }

    public TeleportPayload teleport(Map<String, String> payload) {
        requireExact(payload, TELEPORT, "teleport");
        return new TeleportPayload(
                new ActorId(required(payload, "actorId")),
                new NpcWorldId(required(payload, "worldId")),
                position(payload),
                rotation(payload));
    }

    public DespawnPayload despawn(Map<String, String> payload) {
        requireExact(payload, DESPAWN, "despawn");
        return new DespawnPayload(new ActorId(required(payload, "actorId")));
    }

    public SleepPayload sleepAt(Map<String, String> payload) {
        requireRequiredAndAllowed(payload, SLEEP_AT_REQUIRED, SLEEP_AT, "sleep_at");
        return new SleepPayload(
                new ActorId(required(payload, "npcId")),
                new NpcWorldId(required(payload, "worldId")),
                blockPosition(payload),
                new BlockId(payload.getOrDefault("expectedBlock", ANY_BED)));
    }

    public WakePayload wakeUp(Map<String, String> payload) {
        requireExact(payload, WAKE_UP, "wake_up");
        return new WakePayload(new ActorId(required(payload, "npcId")));
    }

    public String fingerprint(Map<String, String> payload) {
        return payload.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce("", (left, right) -> left.isEmpty() ? right : left + "\n" + right);
    }

    private static void requireExact(Map<String, String> payload, Set<String> required, String action) {
        if (!payload.keySet().equals(required)) {
            throw new IllegalArgumentException("Payload " + action + " must contain exactly: "
                    + String.join(", ", required) + ".");
        }
    }

    private static void requireRequiredAndAllowed(
            Map<String, String> payload,
            Set<String> required,
            Set<String> allowed,
            String action
    ) {
        if (!payload.keySet().containsAll(required) || !allowed.containsAll(payload.keySet())) {
            throw new IllegalArgumentException("Payload " + action + " must contain required arguments: "
                    + String.join(", ", required) + " and may contain: " + String.join(", ", allowed) + ".");
        }
    }

    private static String required(Map<String, String> payload, String key) {
        String value = payload.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Payload argument '" + key + "' is required.");
        }
        return value;
    }

    private static NpcPosition position(Map<String, String> payload) {
        return new NpcPosition(number(payload, "x"), number(payload, "y"), number(payload, "z"));
    }

    private static NpcRotation rotation(Map<String, String> payload) {
        return new NpcRotation((float) number(payload, "yaw"), (float) number(payload, "pitch"));
    }

    private static BlockPosition blockPosition(Map<String, String> payload) {
        return new BlockPosition(integer(payload, "bedX"), integer(payload, "bedY"), integer(payload, "bedZ"));
    }

    private static double number(Map<String, String> payload, String key) {
        try {
            return Double.parseDouble(required(payload, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Payload argument '" + key + "' must be a finite number.",
                    exception);
        }
    }

    private static int integer(Map<String, String> payload, String key) {
        try {
            return Integer.parseInt(required(payload, key));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Payload argument '" + key + "' must be an integer.", exception);
        }
    }
}
