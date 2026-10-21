package io.github.kxyr0.echoplayer.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ContractsArchitectureTest {
    @Test
    void contractsDoNotImportMinecraftForgeOrEchoScript() throws IOException {
        long forbidden = Files.walk(Path.of("src/main/java"))
                .filter(path -> path.toString().endsWith(".java"))
                .map(this::read)
                .filter(text -> text.contains("net.minecraft")
                        || text.contains("net.minecraftforge")
                        || text.contains("io.github.kxyr0.echoscript"))
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
