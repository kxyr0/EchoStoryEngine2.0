package io.github.kxyr0.echoanimationauthoring.draft;

import java.nio.file.Path;
import java.util.Objects;

final class DraftSourcePath {
    Path clipPath(Path workspace, String clipId) {
        Objects.requireNonNull(workspace, "workspace");
        String[] parts = clipId.split(":", 2);
        if (parts.length != 2 || invalid(parts[0]) || invalidPath(parts[1])) {
            throw new IllegalArgumentException("Invalid animation clip id: " + clipId);
        }
        return workspace.resolve("data")
                .resolve(parts[0])
                .resolve("echo_animation")
                .resolve("animation_clips")
                .resolve(parts[1] + ".animation-clip.es")
                .toAbsolutePath()
                .normalize();
    }

    private static boolean invalid(String value) {
        return !value.matches("[a-z0-9_.-]+");
    }

    private static boolean invalidPath(String value) {
        return value.isBlank() || value.contains("..") || !value.matches("[a-z0-9_./-]+");
    }
}
