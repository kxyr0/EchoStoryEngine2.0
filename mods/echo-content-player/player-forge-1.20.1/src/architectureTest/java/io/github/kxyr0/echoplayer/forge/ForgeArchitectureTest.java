package io.github.kxyr0.echoplayer.forge;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ForgeArchitectureTest {
    @Test
    void forgeRuntimeDoesNotImportForeignEchoEngines() throws IOException {
        long forbidden = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> !path.toString().replace('\\', '/')
                        .contains("/io/github/kxyr0/echoplayer/forge/dialogue/"))
                .map(this::read)
                .filter(text -> text.contains("echostoryengine")
                        || text.contains("echodialogue")
                        || text.contains("echonpc")
                        || text.contains("echoanimation")
                        || text.contains("echotrigger")
                        || text.contains("echoitem")
                        || text.contains("echowardrobe"))
                .count();

        assertEquals(0L, forbidden);
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
