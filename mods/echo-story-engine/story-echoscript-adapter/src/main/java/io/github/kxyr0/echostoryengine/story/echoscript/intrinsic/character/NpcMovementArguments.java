package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.character;

import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class NpcMovementArguments {
    private static final String OVERWORLD = "minecraft:overworld";

    Map<String, String> read(IntrinsicCallReader reader, String intrinsic, int offset, String gait) {
        counts(reader, intrinsic, offset);
        reader.rejectUnknownNamed(intrinsic, Set.of("npc", "dimension", "x", "y", "z", "position",
                "targetPosition", "destination", "speed", "speedBlocksPerSecond", "syncAnimation",
                "synchronizeAnimation", "next", "output", "await", "wait"));
        Map<String, String> values = position(reader, intrinsic, offset);
        values.put("gait", gait);
        put(values, "speed", reader.optionalNamedScalar("speed", intrinsic)
                .or(() -> reader.optionalNamedScalar("speedBlocksPerSecond", intrinsic)));
        if (!values.containsKey("speed")) {
            int index = offset + (reader.positionalCount() <= offset + 3 ? 2 : 4);
            put(values, "speed", reader.positionalScalar(index, intrinsic, "speed"));
        }
        put(values, "syncAnimation", reader.optionalNamedScalar("syncAnimation", intrinsic)
                .or(() -> reader.optionalNamedScalar("synchronizeAnimation", intrinsic)));
        put(values, "await", await(reader, intrinsic));
        return values;
    }

    private Map<String, String> position(IntrinsicCallReader reader, String intrinsic, int offset) {
        if (reader.positionalCount() == offset + 2 || reader.positionalCount() == offset + 3) {
            Map<String, String> values = new LinkedHashMap<>();
            put(values, "npc", reader.positionalScalar(offset, intrinsic, "npc"));
            reader.positionalVector(offset + 1, intrinsic, "position").ifPresent(coordinates -> {
                values.put("x", coordinates.get(0)); values.put("y", coordinates.get(1));
                values.put("z", coordinates.get(2));
            });
            put(values, "dimension", reader.optionalNamedScalar("dimension", intrinsic)
                    .or(() -> Optional.of(OVERWORLD)));
            return values;
        }
        Map<String, String> values = new LinkedHashMap<>();
        put(values, "npc", reader.positionalScalar(offset, intrinsic, "npc"));
        put(values, "x", reader.positionalScalar(offset + 1, intrinsic, "x"));
        put(values, "y", reader.positionalScalar(offset + 2, intrinsic, "y"));
        put(values, "z", reader.positionalScalar(offset + 3, intrinsic, "z"));
        put(values, "dimension", reader.optionalNamedScalar("dimension", intrinsic).or(() -> Optional.of(OVERWORLD)));
        return values;
    }

    private void counts(IntrinsicCallReader reader, String intrinsic, int offset) {
        reader.requirePositionalCount(intrinsic, offset == 0 ? Set.of(2, 3, 4, 5) : Set.of(1, 3, 4, 5, 6));
    }

    private Optional<String> await(IntrinsicCallReader reader, String intrinsic) {
        Optional<String> value = reader.optionalNamedScalar("await", intrinsic);
        if (value.isPresent()) {
            return value;
        }
        Optional<String> wait = reader.optionalNamedScalar("wait", intrinsic);
        if (wait.isEmpty()) {
            return Optional.of("STARTED");
        }
        return switch (wait.orElseThrow().trim().toLowerCase(java.util.Locale.ROOT)) {
            case "true" -> Optional.of("ARRIVED");
            case "false" -> Optional.of("STARTED");
            default -> wait;
        };
    }

    private void put(Map<String, String> values, String key, Optional<String> value) {
        value.ifPresent(present -> values.put(key, present));
    }
}
