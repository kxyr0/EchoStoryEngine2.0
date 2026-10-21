package io.github.kxyr0.echoanimation.forge.face;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

record FaceAnimationCommandPayload(String subjectId, String animationId) {
    FaceAnimationCommandPayload {
        subjectId = require(subjectId, "subjectId");
        animationId = require(animationId, "animationId");
    }

    static FaceAnimationCommandPayload decode(String encoded) {
        Map<String, String> values = query(encoded);
        String subject = subject(values);
        String animation = first(values, "animation", "animationId", "faceAnimation", "faceAnimationId")
                .orElseThrow(() -> new IllegalArgumentException("Missing animation."));
        return new FaceAnimationCommandPayload(subject, animation);
    }

    static String decodeSubject(String encoded) {
        return subject(query(encoded));
    }

    private static String subject(Map<String, String> values) {
        return first(values, "subject", "npc", "npcId", "actor")
                .orElseThrow(() -> new IllegalArgumentException("Missing subject."));
    }

    private static Map<String, String> query(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return Map.of();
        }
        Map<String, String> output = new LinkedHashMap<>();
        for (String part : encoded.split("&", -1)) {
            if (!part.isEmpty()) {
                readPart(output, part);
            }
        }
        return Map.copyOf(output);
    }

    private static void readPart(Map<String, String> output, String part) {
        int separator = part.indexOf('=');
        String key = separator >= 0 ? part.substring(0, separator) : part;
        String value = separator >= 0 ? part.substring(separator + 1) : "";
        output.put(decodePart(key), decodePart(value));
    }

    private static Optional<String> first(Map<String, String> values, String... names) {
        for (String name : names) {
            String value = values.get(name);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static String decodePart(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static String require(String value, String name) {
        String trimmed = Objects.requireNonNull(value, name).trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return trimmed;
    }
}
