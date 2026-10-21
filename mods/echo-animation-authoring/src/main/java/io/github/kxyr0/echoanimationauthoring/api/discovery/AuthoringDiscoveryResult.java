package io.github.kxyr0.echoanimationauthoring.api.discovery;

import java.util.List;
import java.util.Objects;

public record AuthoringDiscoveryResult(
        Status status,
        List<String> subjects,
        List<String> clips,
        List<String> rigs,
        List<String> bones,
        List<String> faceElements,
        String message
) {
    public AuthoringDiscoveryResult {
        status = Objects.requireNonNull(status, "status");
        subjects = List.copyOf(Objects.requireNonNull(subjects, "subjects"));
        clips = List.copyOf(Objects.requireNonNull(clips, "clips"));
        rigs = List.copyOf(Objects.requireNonNull(rigs, "rigs"));
        bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        faceElements = List.copyOf(Objects.requireNonNull(faceElements, "faceElements"));
        message = Objects.requireNonNull(message, "message");
    }

    public enum Status {
        DISCOVERED,
        FALLBACK,
        SERVICE_UNAVAILABLE
    }
}
