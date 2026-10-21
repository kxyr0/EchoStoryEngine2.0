package io.github.kxyr0.echoobservation.authoring.core;

import java.nio.file.Path;
import java.util.Optional;

public final class ObservationSourcePath {
    private final Path packRoot;

    public ObservationSourcePath(Path gameDirectory) {
        packRoot = gameDirectory.resolve("story").resolve("content-pack").resolve("jdh")
                .toAbsolutePath().normalize();
    }

    public Optional<Path> resolve(String sourceId) {
        int separator = sourceId.indexOf(':');
        if (separator < 1 || separator == sourceId.length() - 1) return Optional.empty();
        String namespace = sourceId.substring(0, separator);
        if (!namespace.matches("[a-z0-9_.-]+")) return Optional.empty();
        String relative = sourceId.substring(separator + 1).replace('/', java.io.File.separatorChar);
        if (!relative.startsWith("echo_observation" + java.io.File.separator + "observations")
                || !relative.endsWith(".es")) return Optional.empty();
        Path observationRoot = packRoot.resolve("data").resolve(namespace)
                .resolve("echo_observation").resolve("observations").normalize();
        Path target = packRoot.resolve("data").resolve(namespace).resolve(relative).normalize();
        return observationRoot.startsWith(packRoot) && target.startsWith(observationRoot)
                ? Optional.of(target) : Optional.empty();
    }
}
