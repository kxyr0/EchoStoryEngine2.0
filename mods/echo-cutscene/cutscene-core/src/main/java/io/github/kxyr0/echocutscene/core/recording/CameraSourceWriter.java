package io.github.kxyr0.echocutscene.core.recording;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

public final class CameraSourceWriter {
    public Path writeAtomic(Path target, String source) throws IOException {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(source, "source");
        Files.createDirectories(target.getParent());
        Path temp = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temp, source, StandardCharsets.UTF_8);
        return Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
}
