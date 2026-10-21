package io.github.kxyr0.echoitem.core.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Reads author-owned external item-pack content from gameDir/story/item-pack.
 */
public final class ExternalItemSource {
    private final Path itemPackRoot;

    public ExternalItemSource(Path gameDir) {
        Objects.requireNonNull(gameDir, "gameDir");
        this.itemPackRoot = gameDir.resolve("story").resolve("item-pack").normalize();
    }

    public Path root() {
        return itemPackRoot;
    }

    public List<ItemSourceCandidate> weaponCandidates() throws IOException {
        if (!Files.isDirectory(itemPackRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(itemPackRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".weapon.es"))
                    .sorted(Comparator.comparing(path -> itemPackRoot.relativize(path).toString()))
                    .map(this::read)
                    .toList();
        }
    }

    private ItemSourceCandidate read(Path path) {
        try {
            String id = itemPackRoot.relativize(path).toString().replace('\\', '/');
            return new ItemSourceCandidate(id, Files.readString(path, StandardCharsets.UTF_8),
                    ItemSourceOrigin.EXTERNAL);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read external item source: " + path, ex);
        }
    }
}
