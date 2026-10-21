package io.github.kxyr0.echowardrobe.forge.content;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * Lightweight modification stamp for external wardrobe scripts.
 */
public final class ForgeWardrobeScriptStamp {
    private final Path gameDir;

    public ForgeWardrobeScriptStamp(Path gameDir) {
        this.gameDir = gameDir;
    }

    public long current() {
        Path root = gameDir.resolve("story").resolve("content-pack");
        if (!Files.isDirectory(root)) {
            return 0L;
        }
        long stamp = 17L;
        for (Path script : scripts(root)) {
            stamp = combine(stamp, script);
        }
        return stamp;
    }

    private List<Path> scripts(Path contentPackRoot) {
        try (var packs = Files.list(contentPackRoot)) {
            return packs.filter(Files::isDirectory)
                    .flatMap(pack -> scriptRoots(pack).stream())
                    .filter(Files::isDirectory)
                    .flatMap(ForgeWardrobeScriptStamp::walk)
                    .filter(Files::isRegularFile)
                    .filter(ForgeWardrobeScriptStamp::script)
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private List<Path> scriptRoots(Path pack) {
        Path root = pack.resolve("echo_wardrobe");
        return List.of(root.resolve("profiles"), root.resolve("wearables"), root.resolve("outfits"));
    }

    private static java.util.stream.Stream<Path> walk(Path root) {
        try {
            return Files.walk(root);
        } catch (IOException exception) {
            return java.util.stream.Stream.empty();
        }
    }

    private static boolean script(Path path) {
        String name = path.getFileName().toString();
        return name.endsWith(".wearable.es") || name.endsWith(".outfit.es")
                || name.endsWith(".wardrobe-profile.es");
    }

    private long combine(long stamp, Path path) {
        try {
            stamp = 31L * stamp + path.toString().hashCode();
            stamp = 31L * stamp + Files.getLastModifiedTime(path).toMillis();
            return 31L * stamp + Files.size(path);
        } catch (IOException exception) {
            return 31L * stamp + exception.getClass().getName().hashCode();
        }
    }
}
