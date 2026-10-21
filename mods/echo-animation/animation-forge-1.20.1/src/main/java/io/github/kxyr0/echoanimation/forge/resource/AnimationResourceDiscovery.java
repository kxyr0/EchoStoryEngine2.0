package io.github.kxyr0.echoanimation.forge.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
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
 * Discovers Echo Animation data resources with explicit suffixes only.
 */
public final class AnimationResourceDiscovery {
    private static final List<String> ROOTS = List.of(
            "echo_animation/face_rigs",
            "echo_animation/face_poses",
            "echo_animation/face_animations",
            "echo_animation/animations",
            "echo_animation/animation_profiles",
            "echo_animation/animation_rigs",
            "echo_animation/animation_clips");

    public List<AnimationResourceText> discover(ResourceManager resourceManager) {
        List<AnimationResourceText> resources = new ArrayList<>();
        for (String root : ROOTS) {
            discoverRoot(resourceManager, root, resources);
        }
        return List.copyOf(resources);
    }

    public List<AnimationResourceText> discover(ResourceManager resourceManager, Path externalContentPackRoot) {
        Map<String, AnimationResourceText> merged = new LinkedHashMap<>();
        discover(resourceManager).forEach(resource -> merged.put(resource.sourceId(), resource));
        discoverExternal(externalContentPackRoot).forEach(resource -> merged.put(resource.sourceId(), resource));
        return List.copyOf(merged.values());
    }

    private void discoverRoot(ResourceManager manager, String root, List<AnimationResourceText> resources) {
        Map<ResourceLocation, Resource> found = manager.listResources(root, location -> supported(location.getPath()));
        found.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString)))
                .forEach(entry -> readOne(entry.getKey(), entry.getValue(), resources));
    }

    private boolean supported(String path) {
        return path.endsWith(".face-rig.es")
                || path.endsWith(".face-pose.es")
                || path.endsWith(".face-animation.es")
                || path.endsWith(".animation.es")
                || path.endsWith(".animation-profile.es")
                || path.endsWith(".animation-rig.es")
                || path.endsWith(".animation-clip.es");
    }

    List<AnimationResourceText> discoverExternal(Path contentPackRoot) {
        if (contentPackRoot == null) {
            return List.of();
        }
        Path root = contentPackRoot.toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        try (var stream = Files.find(root, 12, (path, attributes) ->
                attributes.isRegularFile() && supported(externalResourcePath(root, path)))) {
            return stream.sorted().map(path -> readExternal(root, path)).flatMap(java.util.Optional::stream).toList();
        } catch (IOException exception) {
            return List.of();
        }
    }

    private Optional<AnimationResourceText> readExternal(Path root, Path file) {
        String resourcePath = externalResourcePath(root, file);
        if (!validExternalResource(resourcePath)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new AnimationResourceText(externalSourceId(resourcePath),
                    Files.readString(file, StandardCharsets.UTF_8)));
        } catch (IOException exception) {
            return Optional.of(new AnimationResourceText(externalSourceId(resourcePath), ""));
        }
    }

    private boolean validExternalResource(String resourcePath) {
        int slash = resourcePath.indexOf('/');
        if (slash <= 0 || slash == resourcePath.length() - 1) {
            return false;
        }
        String namespace = resourcePath.substring(0, slash);
        String path = resourcePath.substring(slash + 1);
        return namespace.matches("[a-z0-9_.-]+") && ROOTS.stream().anyMatch(path::startsWith) && supported(path);
    }

    private String externalSourceId(String resourcePath) {
        int slash = resourcePath.indexOf('/');
        return resourcePath.substring(0, slash) + ":" + resourcePath.substring(slash + 1);
    }

    private String externalResourcePath(Path root, Path file) {
        return root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private void readOne(ResourceLocation location, Resource resource, List<AnimationResourceText> resources) {
        try (InputStreamReader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            resources.add(new AnimationResourceText(location.toString(), readAll(reader)));
        } catch (IOException exception) {
            resources.add(new AnimationResourceText(location.toString(), ""));
        }
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
