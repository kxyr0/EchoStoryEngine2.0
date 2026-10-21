package io.github.kxyr0.echotime.forge.config;

import io.github.kxyr0.echotime.api.config.TimeConfigDiagnostic;
import io.github.kxyr0.echotime.api.config.TimeConfigResult;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import net.minecraftforge.fml.loading.FMLPaths;

public final class TimeSpeedConfigLoader {
    public static final String RELATIVE_PATH = "story/configs/jdh/echo_time/time-speed.es";
    private static final String TEMPLATE = "/data/jdh/echo_time/config/time-speed.es";
    private final Path path;
    private final TimeSpeedConfigBinder binder = new TimeSpeedConfigBinder();

    public TimeSpeedConfigLoader(Path gameDirectory) {
        path = gameDirectory.resolve(RELATIVE_PATH);
    }

    public static TimeSpeedConfigLoader gameDirectory() {
        return new TimeSpeedConfigLoader(FMLPaths.GAMEDIR.get());
    }

    public void seedIfMissing() throws IOException {
        if (Files.isRegularFile(path)) return;
        Files.createDirectories(path.getParent());
        try (InputStream input = TimeSpeedConfigLoader.class.getResourceAsStream(TEMPLATE)) {
            if (input == null) throw new IOException("Packaged time-speed.es template is missing.");
            Files.copy(input, path);
        }
    }

    public TimeConfigResult load(long revision) {
        if (!Files.isRegularFile(path)) {
            return new TimeConfigResult(Optional.empty(), List.of(new TimeConfigDiagnostic(
                    "ECHO_TIME_CONFIG_MISSING", "time-speed.es is missing; last-known-good retained.",
                    path.toString(), Optional.empty())));
        }
        try {
            return binder.bind(Files.readString(path, StandardCharsets.UTF_8), path.toString(), revision);
        } catch (IOException exception) {
            return new TimeConfigResult(Optional.empty(), List.of(new TimeConfigDiagnostic(
                    "ECHO_TIME_CONFIG_NOT_FOUND", exception.getMessage(), path.toString(), Optional.empty())));
        }
    }

    public Path path() {
        return path;
    }
}
