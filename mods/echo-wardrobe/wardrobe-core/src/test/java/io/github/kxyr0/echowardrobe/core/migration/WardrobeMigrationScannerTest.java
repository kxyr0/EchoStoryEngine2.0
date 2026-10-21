package io.github.kxyr0.echowardrobe.core.migration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class WardrobeMigrationScannerTest {
    @TempDir
    Path tempDir;

    @Test
    void mapsLegacyTextureFolderScriptsIntoCanonicalWearableScripts() throws Exception {
        Path legacy = tempDir.resolve("assets/jdh/textures/echo_wardrobe/wearables/origin")
                .resolve("origin_john_boots.es");
        Files.createDirectories(legacy.getParent());
        Files.writeString(legacy, "legacy");

        List<LegacyWardrobeScript> scripts = new WardrobeMigrationScanner().scan(tempDir, "jdh");

        Assertions.assertEquals(1, scripts.size());
        Assertions.assertEquals(tempDir.resolve("echo_wardrobe/wearables/john/origin/origin_john_boots.wearable.es"),
                scripts.get(0).canonicalPath());
    }
}
