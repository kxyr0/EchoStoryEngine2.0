package io.github.kxyr0.echoplayeractor.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlayerActorArchitectureTest {
    private static final List<String> FORBIDDEN_BODY_TOKENS = List.of(
            "PressButton", "ReleaseButton", "HoldKey", "ReleaseKey", "SimulateInput",
            "ClickMouse", "RightClick", "Keyboard", "GenericAction", "FakePlayer",
            "GameType.SPECTATOR", "GameMode.SPECTATOR");

    @Test
    void contractsAndCoreDoNotImportMinecraftOrForge() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();

        List<String> violations = javaFiles(root.resolve("player-actor-contracts/src/main/java"),
                root.resolve("player-actor-core/src/main/java"))
                .stream()
                .filter(file -> text(file).contains("net.minecraft") || text(file).contains("net.minecraftforge"))
                .map(root::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    @Test
    void productionCodeDoesNotContainForbiddenInputSimulation() throws IOException {
        Path root = Path.of("..").toAbsolutePath().normalize();

        List<String> violations = javaFiles(root).stream()
                .filter(file -> !file.toString().contains("\\build\\"))
                .filter(file -> file.toString().contains("\\src\\main\\java\\"))
                .filter(file -> FORBIDDEN_BODY_TOKENS.stream().anyMatch(token -> text(file).contains(token)))
                .map(root::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), String.join("\n", violations));
    }

    private List<Path> javaFiles(Path... roots) throws IOException {
        try (var stream = java.util.Arrays.stream(roots)
                .flatMap(root -> walk(root).stream())) {
            return stream.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private List<Path> walk(Path root) {
        try (var stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String text(Path file) {
        try {
            return Files.readString(file);
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
