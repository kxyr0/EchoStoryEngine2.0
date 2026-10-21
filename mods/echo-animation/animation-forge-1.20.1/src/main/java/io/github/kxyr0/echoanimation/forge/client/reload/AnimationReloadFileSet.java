package io.github.kxyr0.echoanimation.forge.client.reload;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

final class AnimationReloadFileSet {
    private AnimationReloadFileSet() {
    }

    static long fingerprint(Path root) {
        if (!Files.isDirectory(root)) {
            return 0L;
        }
        try (Stream<Path> stream = Files.find(root, 12,
                (path, attributes) -> attributes.isRegularFile() && supported(path))) {
            long hash = 1125899906842597L;
            for (Path file : stream.sorted().toList()) {
                hash = 31L * hash + file.toString().hashCode();
                hash = 31L * hash + Files.size(file);
                hash = 31L * hash + Files.getLastModifiedTime(file).toMillis();
            }
            return hash;
        } catch (IOException exception) {
            return 0L;
        }
    }

    static boolean supported(Path path) {
        String value = path.getFileName() == null ? "" : path.getFileName().toString();
        return value.endsWith(".face-rig.es")
                || value.endsWith(".face-pose.es")
                || value.endsWith(".face-animation.es")
                || value.endsWith(".animation.es")
                || value.endsWith(".animation-profile.es")
                || value.endsWith(".animation-rig.es")
                || value.endsWith(".animation-clip.es");
    }
}
