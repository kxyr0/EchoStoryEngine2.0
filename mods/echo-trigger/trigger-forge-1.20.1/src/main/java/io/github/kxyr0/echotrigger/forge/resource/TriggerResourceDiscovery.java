package io.github.kxyr0.echotrigger.forge.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class TriggerResourceDiscovery {
    public List<TriggerResourceText> externalTriggers(TriggerContentRoots roots) throws IOException {
        List<TriggerResourceText> resources = new ArrayList<>(externalTriggers(roots.canonicalRoot()));
        resources.addAll(contentPackTriggers(roots.contentPackRoot()));
        return resources.stream().sorted(Comparator.comparing(resource -> resource.path().toString())).toList();
    }

    public List<TriggerResourceText> externalTriggers(Path namespaceRoot) throws IOException {
        if (!Files.isDirectory(namespaceRoot)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(namespaceRoot)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".trigger.es"))
                    .map(this::read)
                    .toList();
        }
    }

    private List<TriggerResourceText> contentPackTriggers(Path contentPackRoot) throws IOException {
        if (!Files.isDirectory(contentPackRoot)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.walk(contentPackRoot)) {
            return stream.filter(this::isTriggerResource)
                    .filter(this::insideEchoTriggerRoot)
                    .map(this::read)
                    .toList();
        }
    }

    private boolean isTriggerResource(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".trigger.es");
    }

    private boolean insideEchoTriggerRoot(Path path) {
        Path previous = null;
        for (Path segment : path) {
            if (previous != null && previous.toString().equals("echo_trigger")
                    && segment.toString().equals("triggers")) {
                return true;
            }
            previous = segment;
        }
        return false;
    }

    private TriggerResourceText read(Path path) {
        try {
            return new TriggerResourceText(path, Files.readString(path, StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read trigger resource: " + path, exception);
        }
    }
}
