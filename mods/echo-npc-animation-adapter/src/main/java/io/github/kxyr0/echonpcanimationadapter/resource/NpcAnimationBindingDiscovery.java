package io.github.kxyr0.echonpcanimationadapter.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Discovers adapter binding resources from datapacks and external content workspace.
 */
public final class NpcAnimationBindingDiscovery {
    private static final String ROOT = "echo_npc_animation/bindings";
    private static final String SUFFIX = ".npc-animation-binding.es";

    public List<NpcAnimationBindingResourceText> discover(ResourceManager manager, Path externalRoot) {
        Map<String, NpcAnimationBindingResourceText> merged = new LinkedHashMap<>();
        discoverDatapacks(manager).forEach(resource -> merged.put(resource.sourceId(), resource));
        discoverExternal(externalRoot).forEach(resource -> merged.put(resource.sourceId(), resource));
        return List.copyOf(merged.values());
    }

    private List<NpcAnimationBindingResourceText> discoverDatapacks(ResourceManager manager) {
        List<NpcAnimationBindingResourceText> result = new ArrayList<>();
        manager.listResources(ROOT, location -> location.getPath().endsWith(SUFFIX))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), result));
        return result;
    }

    private List<NpcAnimationBindingResourceText> discoverExternal(Path externalRoot) {
        if (externalRoot == null || !Files.isDirectory(externalRoot)) {
            return List.of();
        }
        Path root = externalRoot.toAbsolutePath().normalize();
        try (var stream = Files.find(root, 8, (path, attributes) ->
                attributes.isRegularFile() && externalResourcePath(root, path).endsWith(SUFFIX))) {
            return stream.sorted().map(path -> readExternal(root, path)).flatMap(Optional::stream).toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private Optional<NpcAnimationBindingResourceText> readExternal(Path root, Path file) {
        String resourcePath = externalResourcePath(root, file);
        if (!validExternalResource(resourcePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new NpcAnimationBindingResourceText(externalSourceId(resourcePath),
                    Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            return Optional.of(new NpcAnimationBindingResourceText(externalSourceId(resourcePath), ""));
        }
    }

    private void readOne(ResourceLocation location, Resource resource, List<NpcAnimationBindingResourceText> result) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            result.add(new NpcAnimationBindingResourceText(location.toString(), readAll(reader)));
        } catch (IOException exception) {
            result.add(new NpcAnimationBindingResourceText(location.toString(), ""));
        }
    }

    private boolean validExternalResource(String resourcePath) {
        int slash = resourcePath.indexOf('/');
        if (slash <= 0 || slash == resourcePath.length() - 1) {
            return false;
        }
        String namespace = resourcePath.substring(0, slash);
        String path = resourcePath.substring(slash + 1);
        return namespace.matches("[a-z0-9_.-]+") && path.startsWith(ROOT) && path.endsWith(SUFFIX);
    }

    private String externalSourceId(String resourcePath) {
        int slash = resourcePath.indexOf('/');
        return resourcePath.substring(0, slash) + ":" + resourcePath.substring(slash + 1);
    }

    private String externalResourcePath(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String readAll(InputStreamReader reader) throws IOException {
        StringBuilder builder = new StringBuilder();
        char[] buffer = new char[4096];
        int read = reader.read(buffer);
        while (read >= 0) {
            builder.append(buffer, 0, read);
            read = reader.read(buffer);
        }
        return builder.toString();
    }
}
