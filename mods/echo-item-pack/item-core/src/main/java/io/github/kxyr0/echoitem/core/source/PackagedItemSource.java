package io.github.kxyr0.echoitem.core.source;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Reads explicitly listed packaged fallback item content from the runtime JAR.
 */
public final class PackagedItemSource {
    private final ClassLoader classLoader;

    public PackagedItemSource(ClassLoader classLoader) {
        this.classLoader = Objects.requireNonNull(classLoader, "classLoader");
    }

    public List<ItemSourceCandidate> weaponCandidates(List<String> resourcePaths) {
        return resourcePaths.stream().map(this::readResource).toList();
    }

    private ItemSourceCandidate readResource(String path) {
        try (var stream = classLoader.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalArgumentException("Packaged item source not found: " + path);
            }
            String text = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            return new ItemSourceCandidate(path, text, ItemSourceOrigin.PACKAGED);
        } catch (IOException ex) {
            throw new UncheckedIOException("Failed to read packaged item source: " + path, ex);
        }
    }
}
