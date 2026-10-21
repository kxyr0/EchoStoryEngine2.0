package io.github.kxyr0.echoiteminspection.forge.resource;

import io.github.kxyr0.echoiteminspection.compile.InspectionSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraftforge.fml.ModList;
import net.minecraft.server.packs.resources.ResourceManager;

/** Reads packaged definitions and the same author-owned story root style as Echo Item Pack. */
public final class InspectionSources {
    private final Path externalRoot;
    private List<InspectionSource> packaged;
    private List<InspectionSource> resourcePack = List.of();

    public InspectionSources(Path gameDirectory) {
        externalRoot = gameDirectory.resolve("story/item-inspection");
    }

    public List<InspectionSource> read() throws IOException {
        if (packaged == null) packaged = readPackaged();
        var merged = new LinkedHashMap<String, InspectionSource>();
        packaged.forEach(source -> merged.put(source.sourceId(), source));
        resourcePack.forEach(source -> merged.put(source.sourceId(), source));
        for (Path file : scripts(externalRoot)) {
            String relative = externalRoot.relativize(file).toString().replace('\\', '/');
            int separator = relative.indexOf('/');
            if (separator < 1) throw new IOException("Expected <namespace>/<name>.inspection.es: " + file);
            String id = "data/" + relative.substring(0, separator) + "/echo_item_inspection/inspections/"
                    + relative.substring(separator + 1);
            merged.put(id, new InspectionSource(id, Files.readString(file)));
        }
        return List.copyOf(merged.values());
    }

    public Path externalRoot() { return externalRoot; }

    public void resourceReload(ResourceManager manager) throws IOException {
        var candidate = new ArrayList<InspectionSource>();
        for (var entry : manager.listResources("echo_item_inspection/inspections",
                id -> id.getPath().endsWith(".inspection.es")).entrySet()) {
            try (var reader = entry.getValue().openAsReader()) {
                String text = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                candidate.add(new InspectionSource("data/" + entry.getKey().getNamespace() + "/"
                        + entry.getKey().getPath(), text));
            }
        }
        resourcePack = List.copyOf(candidate);
        packaged = null;
    }

    private List<InspectionSource> readPackaged() throws IOException {
        var sources = new ArrayList<InspectionSource>();
        for (var mod : ModList.get().getModFiles()) {
            Path data = mod.getFile().findResource("data");
            for (Path file : scripts(data)) {
                String relative = data.relativize(file).toString().replace('\\', '/');
                if (relative.contains("/echo_item_inspection/inspections/")) {
                    sources.add(new InspectionSource("data/" + relative, Files.readString(file)));
                }
            }
        }
        return List.copyOf(sources);
    }

    private List<Path> scripts(Path root) throws IOException {
        if (Files.notExists(root)) return List.of();
        if (!Files.isDirectory(root) || !Files.isReadable(root)) {
            throw new IOException("Inspection content directory is unreadable: " + root);
        }
        try (var files = Files.walk(root)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".inspection.es"))
                    .sorted(Comparator.comparing(Path::toString)).toList();
        }
    }
}
