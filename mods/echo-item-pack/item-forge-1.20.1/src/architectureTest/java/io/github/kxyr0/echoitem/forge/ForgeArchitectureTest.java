package io.github.kxyr0.echoitem.forge;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForgeArchitectureTest {
    @Test
    void forgeRuntimeDoesNotImportOtherEchoEngineImplementations() throws IOException {
        long forbidden = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.toString().endsWith(".java"))
                .filter(this::containsForbiddenEchoEngine)
                .count();

        assertEquals(0L, forbidden);
    }

    private boolean containsForbiddenEchoEngine(Path path) {
        try {
            String text = Files.readString(path);
            return text.contains("echostoryengine")
                    || text.contains("echodialogue")
                    || text.contains("echonpc")
                    || text.contains("echoanimation");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
