package io.github.kxyr0.echoanimationeditor.animationadapter.authoring;

import java.util.List;
import java.util.Objects;

public record EchoAuthoringDiscovery(
        boolean available,
        List<String> subjects,
        List<String> clips,
        List<String> rigs,
        List<String> bones,
        List<String> faceElements,
        String message
) {
    public EchoAuthoringDiscovery {
        subjects = List.copyOf(Objects.requireNonNull(subjects, "subjects"));
        clips = List.copyOf(Objects.requireNonNull(clips, "clips"));
        rigs = List.copyOf(Objects.requireNonNull(rigs, "rigs"));
        bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        faceElements = List.copyOf(Objects.requireNonNull(faceElements, "faceElements"));
        message = Objects.requireNonNull(message, "message");
    }

    public static EchoAuthoringDiscovery unavailable(String message) {
        return new EchoAuthoringDiscovery(false, List.of(), List.of(), List.of(), List.of(), List.of(), message);
    }
}
