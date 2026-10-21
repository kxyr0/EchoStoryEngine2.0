package io.github.kxyr0.echoitem.forge.resource;

import io.github.kxyr0.echoitem.core.asset.ModelCandidateValidator;
import io.github.kxyr0.echoitem.core.asset.TextureCandidateValidator;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.minecraft.resources.ResourceLocation;

/**
 * Resolves external client asset bytes with previous-good retention for broken overrides.
 */
public final class ClientItemAssetStore {
    private final Path itemPackRoot;
    private final TextureCandidateValidator textures = new TextureCandidateValidator();
    private final ModelCandidateValidator models = new ModelCandidateValidator();
    private final ConcurrentMap<ResourceLocation, byte[]> lastGoodTextures = new ConcurrentHashMap<>();
    private final ConcurrentMap<ResourceLocation, byte[]> lastGoodModels = new ConcurrentHashMap<>();

    public ClientItemAssetStore(Path gameDir) {
        this.itemPackRoot = gameDir.resolve("story").resolve("item-pack").normalize();
    }

    public Optional<byte[]> texture(ResourceLocation location) {
        return bytes(location, lastGoodTextures)
                .filter(bytes -> textures.validate(location.toString(), bytes).valid()
                        || lastGoodTextures.containsKey(location));
    }

    public Optional<byte[]> model(ResourceLocation location) {
        return bytes(location, lastGoodModels)
                .filter(bytes -> models.validate(location.toString(), new String(bytes, StandardCharsets.UTF_8))
                        .valid() || lastGoodModels.containsKey(location));
    }

    public Map<ResourceLocation, byte[]> resources(String namespace, String path) {
        Path namespaceRoot = itemPackRoot.resolve(namespace).resolve("assets").normalize();
        if (!namespaceRoot.startsWith(itemPackRoot) || !Files.isDirectory(namespaceRoot)) {
            return Map.of();
        }
        String normalizedPath = path.replace('\\', '/');
        Map<ResourceLocation, byte[]> found = new LinkedHashMap<>();
        try (var stream = Files.walk(namespaceRoot)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::trackedAsset)
                    .sorted(Comparator.comparing(file -> namespaceRoot.relativize(file).toString()))
                    .forEach(file -> listAsset(namespaceRoot, namespace, normalizedPath, file, found));
        } catch (IOException ex) {
            return found;
        }
        return found;
    }

    public Set<String> namespaces() {
        if (!Files.isDirectory(itemPackRoot)) {
            return Set.of();
        }
        Set<String> found = new TreeSet<>();
        try (var stream = Files.list(itemPackRoot)) {
            stream.filter(Files::isDirectory)
                    .filter(path -> Files.isDirectory(path.resolve("assets")))
                    .map(path -> path.getFileName().toString())
                    .forEach(found::add);
        } catch (IOException ex) {
            return Set.of();
        }
        return found;
    }

    public long externalAssetFingerprint() {
        if (!Files.isDirectory(itemPackRoot)) {
            return 0L;
        }
        try (var stream = Files.walk(itemPackRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(this::trackedAsset)
                    .sorted(Comparator.comparing(path -> itemPackRoot.relativize(path).toString()))
                    .mapToLong(this::fileFingerprint)
                    .reduce(17L, (left, right) -> 31L * left + right);
        } catch (IOException ex) {
            return -1L;
        }
    }

    private Optional<byte[]> bytes(ResourceLocation location, ConcurrentMap<ResourceLocation, byte[]> lastGood) {
        Path path = externalPath(location);
        if (!path.startsWith(itemPackRoot) || !Files.exists(path)) {
            lastGood.remove(location);
            return Optional.empty();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            if (valid(location, bytes)) {
                lastGood.put(location, bytes);
                return Optional.of(bytes);
            }
        } catch (IOException ex) {
            return Optional.ofNullable(lastGood.get(location));
        }
        return Optional.ofNullable(lastGood.get(location));
    }

    private boolean valid(ResourceLocation location, byte[] bytes) {
        if (location.getPath().endsWith(".png")) {
            return textures.validate(location.toString(), bytes).valid();
        }
        if (location.getPath().endsWith(".json")) {
            return models.validate(location.toString(), new String(bytes, StandardCharsets.UTF_8)).valid();
        }
        return false;
    }

    private Path externalPath(ResourceLocation location) {
        return itemPackRoot.resolve(location.getNamespace())
                .resolve("assets")
                .resolve(location.getPath())
                .normalize();
    }

    private boolean trackedAsset(Path path) {
        String relative = itemPackRoot.relativize(path).toString().replace('\\', '/');
        return relative.contains("/assets/") && (relative.endsWith(".png") || relative.endsWith(".json"));
    }

    private void listAsset(Path namespaceRoot, String namespace, String path, Path file,
                           Map<ResourceLocation, byte[]> output) {
        String relative = namespaceRoot.relativize(file).toString().replace('\\', '/');
        if (!relative.startsWith(path)) {
            return;
        }
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(namespace, relative);
        bytes(location, relative.endsWith(".png") ? lastGoodTextures : lastGoodModels)
                .ifPresent(bytes -> output.put(location, bytes));
    }

    private long fileFingerprint(Path path) {
        try {
            String relative = itemPackRoot.relativize(path).toString().replace('\\', '/');
            return relative.hashCode() * 31L + Files.size(path) * 17L
                    + Files.getLastModifiedTime(path).toMillis();
        } catch (IOException ex) {
            return path.toString().hashCode();
        }
    }
}
