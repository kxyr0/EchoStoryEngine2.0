package io.github.kxyr0.echovisibility.forge.resource;

import java.nio.file.Path;
import java.util.Objects;

public record VisibilityContentRoots(Path gameDirectory) {
    public VisibilityContentRoots {
        gameDirectory = Objects.requireNonNull(gameDirectory, "gameDirectory").toAbsolutePath().normalize();
    }

    public Path contentPackRoot() {
        return gameDirectory.resolve("story").resolve("content-pack");
    }
}
