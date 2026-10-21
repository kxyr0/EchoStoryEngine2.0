package io.github.kxyr0.echocore.api;

import java.nio.file.Path;
import java.util.Objects;

public record ChapterDefinition(String id, Path template) {
    public ChapterDefinition {
        if (id == null || !id.matches("[a-z][a-z0-9_]{0,48}")) throw new IllegalArgumentException("Invalid chapter ID: " + id);
        template = Objects.requireNonNull(template, "template").toAbsolutePath().normalize();
    }
    public String saveId() { return "echo_core_" + id; }
}
