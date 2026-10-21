package io.github.kxyr0.echonpcnavigationadapter.script;

import io.github.kxyr0.echonavigation.api.position.NavigationDestination;
import io.github.kxyr0.echonavigation.api.movement.NavigationGait;
import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class NpcNavigationPayloads {
    private static final String ANY_BED = "echo_npc:any_bed";
    public static final double WALK_SPEED_BLOCKS_PER_SECOND = 1.35D;
    public static final double SPRINT_SPEED_BLOCKS_PER_SECOND = 4.3D;

    private NpcNavigationPayloads() {
    }

    public static MoveCommand move(Map<String, String> arguments) {
        return movement(arguments, NavigationGait.WALK);
    }

    public static MoveCommand sprint(Map<String, String> arguments) {
        return movement(arguments, NavigationGait.SPRINT);
    }

    public static AwaitMode awaitMode(Map<String, String> arguments) {
        Optional<String> await = first(arguments, "await", "awaitMode");
        if (await.isPresent()) {
            return AwaitMode.from(await.orElseThrow());
        }
        return first(arguments, "wait", "waitForArrival")
                .map(AwaitMode::fromWaitFlag)
                .orElse(AwaitMode.ARRIVED);
    }

    public static ActorId actor(Map<String, String> arguments) {
        return new ActorId(first(arguments, "npc", "actor", "actorId", "target", "arg0")
                .orElseThrow(() -> new IllegalArgumentException("NPC actor argument is missing.")));
    }

    public static FormCommand form(Map<String, String> arguments) {
        return new FormCommand(actor(arguments), new NpcFormId(first(arguments, "form", "formId", "arg1")
                .orElseThrow(() -> new IllegalArgumentException("NPC form argument is missing."))));
    }

    public static SleepCommand sleep(Map<String, String> arguments) {
        return new SleepCommand(
                actor(arguments),
                new NpcWorldId(first(arguments, "worldId", "world", "dimension", "dim")
                        .orElse("minecraft:overworld")),
                bedPosition(arguments),
                new BlockId(first(arguments, "expectedBlock").orElse(ANY_BED)));
    }

    public static NavigationDestination destination(Map<String, String> arguments) {
        Map<String, String> values = withVector(arguments);
        return new NavigationDestination(
                first(values, "dimension", "dim", "world", "worldId").orElse("minecraft:overworld"),
                intValue(values, "x", "arg1"),
                intValue(values, "y", "arg2"),
                intValue(values, "z", "arg3"));
    }

    private static BlockPosition bedPosition(Map<String, String> arguments) {
        return new BlockPosition(
                intValue(arguments, "bedX", "x", "arg1"),
                intValue(arguments, "bedY", "y", "arg2"),
                intValue(arguments, "bedZ", "z", "arg3"));
    }

    private static int intValue(Map<String, String> arguments, String named, String positional) {
        return intValue(arguments, named, named, positional);
    }

    private static int intValue(Map<String, String> arguments, String named, String alias, String positional) {
        String value = first(arguments, named, alias, positional)
                .orElseThrow(() -> new IllegalArgumentException(named + " argument is missing."));
        return Integer.parseInt(value);
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

    private static MoveCommand movement(Map<String, String> arguments, NavigationGait fallbackGait) {
        NavigationGait gait = first(arguments, "gait")
                .map(value -> NavigationGait.valueOf(value.trim().toUpperCase(Locale.ROOT)))
                .orElse(fallbackGait);
        double authoredSpeed = gait == NavigationGait.SPRINT
                ? SPRINT_SPEED_BLOCKS_PER_SECOND : WALK_SPEED_BLOCKS_PER_SECOND;
        Optional<String> positionalSpeed = first(arguments, "arg1")
                .filter(value -> value.startsWith("Vector3("))
                .flatMap(ignored -> first(arguments, "arg2"))
                .or(() -> first(arguments, "arg4"));
        double speed = first(arguments, "speed", "speedBlocksPerSecond").or(() -> positionalSpeed)
                .map(Double::parseDouble).orElse(authoredSpeed);
        boolean synchronize = first(arguments, "syncAnimation", "synchronizeAnimation")
                .map(Boolean::parseBoolean).orElse(true);
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("NPC movement speed must be finite and positive.");
        }
        return new MoveCommand(actor(arguments), destination(arguments), gait, speed, synchronize);
    }

    private static Map<String, String> withVector(Map<String, String> arguments) {
        if (arguments.containsKey("x")) {
            return arguments;
        }
        Optional<String> raw = first(arguments, "position", "targetPosition", "destination", "arg1");
        if (raw.isEmpty() || !raw.orElseThrow().startsWith("Vector3(") || !raw.orElseThrow().endsWith(")")) {
            return arguments;
        }
        String vector = raw.orElseThrow();
        String[] coordinates = vector.substring("Vector3(".length(), vector.length() - 1).split(",");
        if (coordinates.length != 3) {
            throw new IllegalArgumentException("Vector3 requires x, y and z coordinates.");
        }
        Map<String, String> values = new java.util.LinkedHashMap<>(arguments);
        values.put("x", coordinates[0].trim());
        values.put("y", coordinates[1].trim());
        values.put("z", coordinates[2].trim());
        return values;
    }

    public record MoveCommand(
            ActorId actorId,
            NavigationDestination destination,
            NavigationGait gait,
            double speedBlocksPerSecond,
            boolean synchronizeAnimationSpeed) {
    }

    public enum AwaitMode {
        STARTED,
        ARRIVED;

        static AwaitMode from(String raw) {
            String value = normalized(raw);
            return switch (value) {
                case "START", "STARTED", "ACCEPTED", "DISPATCHED", "FALSE" -> STARTED;
                case "ARRIVE", "ARRIVED", "COMPLETE", "COMPLETED", "TERMINAL", "TRUE" -> ARRIVED;
                default -> throw new IllegalArgumentException("Unsupported NPC move await mode: " + raw);
            };
        }

        static AwaitMode fromWaitFlag(String raw) {
            String value = normalized(raw);
            if ("FALSE".equals(value)) {
                return STARTED;
            }
            if ("TRUE".equals(value)) {
                return ARRIVED;
            }
            return from(raw);
        }

        private static String normalized(String raw) {
            String value = raw == null ? "" : raw.trim();
            if (value.isEmpty()) {
                throw new IllegalArgumentException("NPC move await mode is blank.");
            }
            return value.toUpperCase(Locale.ROOT);
        }
    }

    public record FormCommand(ActorId actorId, NpcFormId formId) {
    }

    public record SleepCommand(
            ActorId actorId,
            NpcWorldId worldId,
            BlockPosition bedPosition,
            BlockId expectedBlock) {
    }
}
