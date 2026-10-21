package io.github.kxyr0.echotrigger.forge.resource;

import java.nio.file.Path;
import java.util.Objects;

public record TriggerContentRoots(Path gameDirectory) {
    public TriggerContentRoots {
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
    }

    public Path externalNamespaceRoot(String namespace) {
        return gameDirectory.resolve("story").resolve("content-pack").resolve(namespace)
                .resolve("echo_trigger").resolve("triggers");
    }

    public Path contentPackRoot() {
        return gameDirectory.resolve("story").resolve("content-pack");
    }

    public Path canonicalRoot() {
        return gameDirectory.resolve("story").resolve("triggers");
    }
}
