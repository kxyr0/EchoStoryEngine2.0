package io.github.kxyr0.echowardrobe.core.migration;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Finds non-canonical wardrobe scripts stored in texture asset folders.
 */
public final class WardrobeMigrationScanner {
    public List<LegacyWardrobeScript> scan(Path contentPackRoot, String namespace) throws IOException {
        Path textureRoot = contentPackRoot.resolve("assets").resolve(namespace)
                .resolve("textures").resolve("echo_wardrobe");
        if (!Files.isDirectory(textureRoot)) {
            return List.of();
        }
        try (var stream = Files.walk(textureRoot)) {
            return stream.filter(path -> path.getFileName().toString().endsWith(".es"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> legacy(contentPackRoot, textureRoot, path))
                    .toList();
        }
    }

    private LegacyWardrobeScript legacy(Path contentPackRoot, Path textureRoot, Path path) {
        String name = path.getFileName().toString().replace(".es", ".wearable.es");
        String collection = collection(textureRoot.relativize(path));
        Path canonical = contentPackRoot.resolve("echo_wardrobe")
                .resolve("wearables").resolve("john").resolve(collection).resolve(name);
        return new LegacyWardrobeScript(path, canonical, empty(path),
                WardrobeDiagnosticCode.WARDROBE_SCRIPT_IN_ASSET_FOLDER.name());
    }

    private String collection(Path relative) {
        if (relative.getNameCount() > 1 && "wearables".equals(relative.getName(0).toString())) {
            return relative.getName(1).toString();
        }
        return relative.getName(0).toString();
    }

    private boolean empty(Path path) {
        try {
            return Files.readString(path).trim().isEmpty();
        } catch (IOException exception) {
            return false;
        }
    }
}
