package io.github.kxyr0.echoitemattachments.script;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.SubjectKind;
import io.github.kxyr0.echoitemattachments.api.model.ReleaseMode;
import io.github.kxyr0.echoitemattachments.api.model.WorldPosition;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

record ItemAttachmentScriptPayload(Map<String, String> values) {
    ItemAttachmentScriptPayload {
        values = Map.copyOf(values);
    }

    AttachmentSubjectRef subject() {
        SubjectKind kind = SubjectKind.valueOf(required("subjectKind").toUpperCase(java.util.Locale.ROOT));
        if (kind == SubjectKind.PLAYER) {
            return new AttachmentSubjectRef(kind, UUID.fromString(required("player")).toString());
        }
        String value = required("npc").trim().toLowerCase(java.util.Locale.ROOT);
        return new AttachmentSubjectRef(kind, value.contains(":") ? value : "jdh:" + value);
    }

    AttachmentPointId hand(String key) {
        String value = required(key).toUpperCase(java.util.Locale.ROOT);
        if (value.endsWith("RIGHT_HAND")) return AttachmentPointId.RIGHT_HAND;
        if (value.endsWith("LEFT_HAND")) return AttachmentPointId.LEFT_HAND;
        throw new IllegalArgumentException(key + " must be LEFT_HAND or RIGHT_HAND");
    }

    WorldPosition position() {
        String encoded = required("position").trim();
        int open = encoded.indexOf('(');
        int close = encoded.lastIndexOf(')');
        if (open < 1 || close != encoded.length() - 1) {
            throw new IllegalArgumentException("position must resolve to Position(x,y,z)");
        }
        String[] parts = encoded.substring(open + 1, close).split(",", -1);
        if (parts.length != 3) throw new IllegalArgumentException("position requires three coordinates");
        return new WorldPosition(values.getOrDefault("dimension", "minecraft:overworld"),
                Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()),
                Double.parseDouble(parts[2].trim()));
    }

    Optional<String> animation() {
        return Optional.ofNullable(values.get("animation")).map(this::namespaced);
    }

    Optional<String> expected() { return Optional.ofNullable(values.get("expected")); }
    int count() { return Integer.parseInt(values.getOrDefault("count", "1")); }
    double radius() { return Double.parseDouble(values.getOrDefault("radius", "0.75")); }
    double speed() { return Double.parseDouble(values.getOrDefault("speed", "1.0")); }
    ReleaseMode release() { return ReleaseMode.valueOf(values.getOrDefault("release", "LOOK_DIRECTION")); }

    private String required(String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException(key + " argument is missing");
        return value;
    }

    private String namespaced(String value) {
        String normalized = value.trim().replaceAll("([a-z])([A-Z])", "$1_$2")
                .toLowerCase(java.util.Locale.ROOT);
        return normalized.contains(":") ? normalized : "jdh:" + normalized;
    }
}
