package io.github.kxyr0.echoitem.core.manifest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

/**
 * Atomic JSON persistence for config/echo-item-pack/registered-items.json.
 */
public final class RegisteredItemManifestStore {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path manifestPath;

    public RegisteredItemManifestStore(Path gameDir) {
        Objects.requireNonNull(gameDir, "gameDir");
        this.manifestPath = gameDir.resolve("config").resolve("echo-item-pack").resolve("registered-items.json");
    }

    public Path path() {
        return manifestPath;
    }

    public RegisteredItemManifest load() {
        if (!Files.isRegularFile(manifestPath)) {
            return RegisteredItemManifest.empty();
        }
        try {
            ManifestDto dto = gson.fromJson(Files.readString(manifestPath, StandardCharsets.UTF_8), ManifestDto.class);
            return dto == null ? RegisteredItemManifest.empty() : dto.toManifest();
        } catch (RuntimeException | IOException ex) {
            return RegisteredItemManifest.empty();
        }
    }

    public void write(RegisteredItemManifest manifest) throws IOException {
        Files.createDirectories(manifestPath.getParent());
        Path temp = manifestPath.resolveSibling(manifestPath.getFileName() + ".tmp");
        Files.writeString(temp, gson.toJson(ManifestDto.from(manifest)) + "\n", StandardCharsets.UTF_8);
        Files.move(temp, manifestPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    private record ManifestDto(List<RegisteredItemRecord> records) {
        RegisteredItemManifest toManifest() {
            return new RegisteredItemManifest(records == null ? List.of() : records);
        }

        static ManifestDto from(RegisteredItemManifest manifest) {
            return new ManifestDto(manifest.records());
        }
    }
}
