package io.github.kxyr0.echotrigger.forge.resource;

import java.nio.file.Path;
import java.util.Objects;

public record TriggerResourceText(Path path, String text) {
    public TriggerResourceText {
        path = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        text = Objects.requireNonNull(text, "text");
    }
}
