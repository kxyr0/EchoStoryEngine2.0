package io.github.kxyr0.echovisibility.forge.resource;

import java.nio.file.Path;
import java.util.Objects;

public record VisibilityResourceText(Path path, String text) {
    public VisibilityResourceText {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        text = Objects.requireNonNull(text, "text");
    }
}
