package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep;

import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class SleepDreamTargetArguments {
    private static final String OVERWORLD = "minecraft:overworld";

    Map<String, String> read(IntrinsicCallReader reader, String intrinsic, int firstPayloadIndex) {
        return read(reader, intrinsic, firstPayloadIndex, Map.of());
    }

    Map<String, String> read(
            IntrinsicCallReader reader,
            String intrinsic,
            int firstPayloadIndex,
            Map<String, String> blackspaceVariables) {
        int count = reader.positionalCount() - firstPayloadIndex;
        if (count == 0) {
            return readNamed(reader, intrinsic);
        }
        if (count == 1) {
            return readTarget(reader, intrinsic, firstPayloadIndex, blackspaceVariables);
        }
        return readCoordinates(reader, intrinsic, firstPayloadIndex, count);
    }

    private Map<String, String> readNamed(IntrinsicCallReader reader, String intrinsic) {
        Map<String, String> arguments = new LinkedHashMap<>();
        putNamedBlackspace(arguments, reader, intrinsic);
        if (arguments.containsKey("blackspaceInstanceId")) {
            return arguments;
        }
        put(arguments, "dimension", namedDimension(reader, intrinsic, true));
        putNamedTarget(arguments, reader, intrinsic);
        return arguments;
    }

    private Map<String, String> readTarget(
            IntrinsicCallReader reader,
            String intrinsic,
            int firstPayloadIndex,
            Map<String, String> blackspaceVariables) {
        Map<String, String> arguments = new LinkedHashMap<>();
        Optional<String> target = reader.positionalReference(firstPayloadIndex, intrinsic, "target");
        if (target.isPresent() && blackspaceVariables.containsKey(target.orElseThrow())) {
            arguments.put("blackspaceInstanceId", blackspaceVariables.get(target.orElseThrow()));
        } else {
            put(arguments, "target", target);
        }
        return arguments;
    }

    private Map<String, String> readCoordinates(
            IntrinsicCallReader reader,
            String intrinsic,
            int firstPayloadIndex,
            int count) {
        Map<String, String> arguments = new LinkedHashMap<>();
        boolean explicitDimension = count == 4 || count == 6;
        int coordinateIndex = explicitDimension ? firstPayloadIndex + 1 : firstPayloadIndex;
        put(arguments, "dimension", explicitDimension
                ? reader.positionalScalar(firstPayloadIndex, intrinsic, "dimension")
                : namedDimension(reader, intrinsic, false));
        put(arguments, "x", reader.positionalScalar(coordinateIndex, intrinsic, "x"));
        put(arguments, "y", reader.positionalScalar(coordinateIndex + 1, intrinsic, "y"));
        put(arguments, "z", reader.positionalScalar(coordinateIndex + 2, intrinsic, "z"));
        if (count == 5 || count == 6) {
            put(arguments, "yaw", reader.positionalScalar(coordinateIndex + 3, intrinsic, "yaw"));
            put(arguments, "pitch", reader.positionalScalar(coordinateIndex + 4, intrinsic, "pitch"));
        } else {
            put(arguments, "yaw", reader.requiredNamedScalar("yaw", intrinsic));
            put(arguments, "pitch", reader.requiredNamedScalar("pitch", intrinsic));
        }
        return arguments;
    }

    private void putNamedBlackspace(Map<String, String> arguments, IntrinsicCallReader reader, String intrinsic) {
        Optional<String> blackspace = reader.optionalNamedScalar("blackspaceInstanceId", intrinsic)
                .or(() -> reader.optionalNamedScalar("blackspaceId", intrinsic));
        put(arguments, "blackspaceInstanceId", blackspace);
    }

    private Optional<String> namedDimension(IntrinsicCallReader reader, String intrinsic, boolean required) {
        Optional<String> dimension = reader.optionalNamedScalar("dimension", intrinsic)
                .or(() -> reader.optionalNamedScalar("worldId", intrinsic));
        if (dimension.isPresent()) {
            return dimension;
        }
        return required ? reader.requiredNamedScalar("dimension", intrinsic) : Optional.of(OVERWORLD);
    }

    private void putNamedTarget(Map<String, String> arguments, IntrinsicCallReader reader, String intrinsic) {
        put(arguments, "x", reader.requiredNamedScalar("x", intrinsic));
        put(arguments, "y", reader.requiredNamedScalar("y", intrinsic));
        put(arguments, "z", reader.requiredNamedScalar("z", intrinsic));
        put(arguments, "yaw", reader.requiredNamedScalar("yaw", intrinsic));
        put(arguments, "pitch", reader.requiredNamedScalar("pitch", intrinsic));
    }

    private void put(Map<String, String> map, String key, Optional<String> value) {
        value.ifPresent(present -> map.put(key, present));
    }
}
