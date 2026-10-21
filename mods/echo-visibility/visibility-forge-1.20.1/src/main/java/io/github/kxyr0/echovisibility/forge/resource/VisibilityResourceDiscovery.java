package io.github.kxyr0.echovisibility.forge.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

public final class VisibilityResourceDiscovery {
    public List<VisibilityResourceText> discover(Path contentPackRoot) throws IOException {
        if (!Files.isDirectory(contentPackRoot)) {
            return List.of();
        }
        try (Stream<Path> namespaces = Files.list(contentPackRoot)) {
            return namespaces.filter(Files::isDirectory).flatMap(this::watchFiles)
                    .sorted().map(this::read).toList();
        }
    }

    private Stream<Path> watchFiles(Path namespace) {
        Path root = namespace.resolve("echo_visibility").resolve("watches");
        if (!Files.isDirectory(root)) {
            return Stream.empty();
        }
        try {
            return Files.walk(root).filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".es"));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to scan visibility watches: " + root, exception);
        }
    }

    private VisibilityResourceText read(Path path) {
        try {
            return new VisibilityResourceText(path, Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read visibility watch: " + path, exception);
        }
    }
}
