package io.github.kxyr0.echostoryengine.story.forge.resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

record FakeReloadResourceManager(Map<ResourceLocation, Resource> resources) implements ResourceManager {
    static Resource resource(String text) {
        return new Resource(new FakePackResources(), () -> new ByteArrayInputStream(
                text.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public Optional<Resource> getResource(ResourceLocation location) {
        return Optional.ofNullable(resources.get(location));
    }

    @Override
    public Set<String> getNamespaces() {
        return resources.keySet().stream().map(ResourceLocation::getNamespace).collect(Collectors.toSet());
    }

    @Override
    public List<Resource> getResourceStack(ResourceLocation location) {
        return getResource(location).stream().toList();
    }

    @Override
    public Map<ResourceLocation, Resource> listResources(
            String path,
            java.util.function.Predicate<ResourceLocation> filter) {
        Map<ResourceLocation, Resource> found = new LinkedHashMap<>();
        resources.forEach((location, resource) -> {
            if (location.getPath().startsWith(path) && filter.test(location)) {
                found.put(location, resource);
            }
        });
        return found;
    }

    @Override
    public Map<ResourceLocation, List<Resource>> listResourceStacks(
            String path,
            java.util.function.Predicate<ResourceLocation> filter) {
        return listResources(path, filter).entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> List.of(entry.getValue())));
    }

    @Override
    public Stream<PackResources> listPacks() {
        return Stream.of(new FakePackResources());
    }

    private static final class FakePackResources implements PackResources {
        @Override
        public IoSupplier<InputStream> getRootResource(String... path) {
            return null;
        }

        @Override
        public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
            return null;
        }

        @Override
        public void listResources(
                PackType type,
                String namespace,
                String path,
                PackResources.ResourceOutput output) {
        }

        @Override
        public Set<String> getNamespaces(PackType type) {
            return Set.of("sample");
        }

        @Override
        public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) throws IOException {
            return null;
        }

        @Override
        public String packId() {
            return "echo-story-test-pack";
        }

        @Override
        public void close() {
        }
    }
}
