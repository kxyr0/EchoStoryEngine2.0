package io.github.kxyr0.echoseating.forge.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class SeatZoneResourceDiscovery {
    private SeatZoneResourceDiscovery() {
    }

    public static List<SeatZoneResourceText> discover(Path worldRoot) {
        List<SeatZoneResourceText> resources = new ArrayList<>();
        for (Path contentRoot : contentRoots(worldRoot)) {
            resources.addAll(discoverContentRoot(contentRoot));
        }
        return List.copyOf(resources);
    }

    private static List<Path> contentRoots(Path worldRoot) {
        Path normalized = worldRoot.toAbsolutePath().normalize();
        List<Path> roots = new ArrayList<>();
        addIfDirectory(roots, normalized.resolve("story").resolve("content-pack"));
        Path saves = normalized.getParent();
        Path gameDirectory = saves == null ? null : saves.getParent();
        if (gameDirectory != null) {
            addIfDirectory(roots, gameDirectory.resolve("story").resolve("content-pack"));
        }
        return List.copyOf(roots);
    }

    private static void addIfDirectory(List<Path> roots, Path root) {
        if (Files.isDirectory(root) && !roots.contains(root)) {
            roots.add(root);
        }
    }

    private static List<SeatZoneResourceText> discoverContentRoot(Path contentRoot) {
        try (Stream<Path> stream = Files.walk(contentRoot)) {
            return stream.filter(path -> path.toString().endsWith(".es"))
                    .filter(path -> path.toString().replace('\\', '/').contains("/echo_seating/zones/"))
                    .map(path -> read(contentRoot, path))
                    .flatMap(List::stream)
                    .toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private static List<SeatZoneResourceText> read(Path root, Path path) {
        try {
            return List.of(new SeatZoneResourceText(root.relativize(path).toString().replace('\\', '/'),
                    Files.readString(path, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            return List.of();
        }
    }
}
