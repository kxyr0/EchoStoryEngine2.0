package io.github.kxyr0.echoanimationeditor.forge.client;

import io.github.kxyr0.echoanimationeditor.animationadapter.authoring.EchoAuthoringDiscovery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AnimatorDiscoveryState(
        List<String> subjects,
        List<String> clips,
        List<String> rigs,
        List<String> bones,
        List<String> faceElements,
        String message
) {
    public AnimatorDiscoveryState {
        subjects = List.copyOf(Objects.requireNonNull(subjects, "subjects"));
        clips = List.copyOf(Objects.requireNonNull(clips, "clips"));
        rigs = List.copyOf(Objects.requireNonNull(rigs, "rigs"));
        bones = List.copyOf(Objects.requireNonNull(bones, "bones"));
        faceElements = List.copyOf(Objects.requireNonNull(faceElements, "faceElements"));
        message = Objects.requireNonNull(message, "message");
    }

    static AnimatorDiscoveryState from(EchoAuthoringDiscovery discovery) {
        return new AnimatorDiscoveryState(discovery.subjects(), discovery.clips(), discovery.rigs(),
                discovery.bones(), discovery.faceElements(), discovery.message());
    }

    public Optional<String> firstBone() {
        return bones.stream().findFirst();
    }

    public Optional<String> firstFaceElement() {
        return faceElements.stream().findFirst();
    }
}
