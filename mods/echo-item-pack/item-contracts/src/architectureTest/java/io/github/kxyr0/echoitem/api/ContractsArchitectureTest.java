package io.github.kxyr0.echoitem.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ContractsArchitectureTest {
    @Test
    void contractsDoNotImportMinecraftForgeOrEchoScript() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        long forbidden = Files.walk(sourceRoot)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(this::containsForbiddenImport)
                .count();

        assertEquals(0L, forbidden);
    }

    private boolean containsForbiddenImport(Path path) {
        try {
            String text = Files.readString(path);
            return text.contains("net.minecraft")
                    || text.contains("net.minecraftforge")
                    || text.contains("io.github.kxyr0.echoscript");
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
