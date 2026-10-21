package io.github.kxyr0.echoitem.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreArchitectureTest {
    @Test
    void coreDoesNotImportMinecraftForgeOrOtherEchoEngines() throws IOException {
        long forbidden = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.toString().endsWith(".java"))
                .filter(this::containsForbidden)
                .count();

        assertEquals(0L, forbidden);
    }

    private boolean containsForbidden(Path path) {
        try {
            String text = Files.readString(path);
            return text.contains("net.minecraft")
                    || text.contains("net.minecraftforge")
                    || text.contains("echostoryengine")
                    || text.contains("echodialogue")
                    || text.contains("echonpc");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
