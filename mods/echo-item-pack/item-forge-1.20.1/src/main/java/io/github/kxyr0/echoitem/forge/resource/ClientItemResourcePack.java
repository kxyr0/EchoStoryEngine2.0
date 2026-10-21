package io.github.kxyr0.echoitem.forge.resource;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import io.github.kxyr0.echoitem.forge.EchoItemPackMod;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraftforge.event.AddPackFindersEvent;

/**
 * Dynamic client resource pack for external assets and generated item models.
 */
public final class ClientItemResourcePack implements PackResources {
    private static final String PACK_ID = EchoItemPackMod.MOD_ID + ":dynamic_assets";
    private static final int PACK_FORMAT = 15;
    private final WeaponDefinitionCatalog catalog;
    private final ClientItemAssetStore assets;

    public ClientItemResourcePack(WeaponDefinitionCatalog catalog, ClientItemAssetStore assets) {
        this.catalog = java.util.Objects.requireNonNull(catalog, "catalog");
        this.assets = java.util.Objects.requireNonNull(assets, "assets");
    }

    public void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            event.addRepositorySource(output -> output.accept(pack()));
        }
    }

    @Override
    public IoSupplier<InputStream> getRootResource(String... path) {
        return path.length == 1 && "pack.mcmeta".equals(path[0]) ? stream(packMeta()) : null;
    }

    @Override
    public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
        if (type != PackType.CLIENT_RESOURCES) {
            return null;
        }
        if (location.getPath().startsWith("textures/")) {
            return assets.texture(location).map(ClientItemResourcePack::stream).orElse(null);
        }
        if (location.getPath().startsWith("models/")) {
            return modelBytes(location).map(ClientItemResourcePack::stream).orElse(null);
        }
        return null;
    }

    @Override
    public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
        if (type != PackType.CLIENT_RESOURCES) {
            return;
        }
        Set<ResourceLocation> emitted = new HashSet<>();
        assets.resources(namespace, path).forEach((location, bytes) -> {
            emitted.add(location);
            output.accept(location, stream(bytes));
        });
        catalog.snapshot().weapons().values().forEach(definition -> listDefinition(namespace, path, output,
                emitted, definition));
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        Set<String> namespaces = new TreeSet<>();
        if (type == PackType.CLIENT_RESOURCES) {
            namespaces.addAll(assets.namespaces());
            catalog.snapshot().weapons().values().forEach(definition -> {
                namespaces.add(definition.id().namespace());
                textureLocation(definition.visual().texture()).map(ResourceLocation::getNamespace)
                        .ifPresent(namespaces::add);
                definition.visual().model().flatMap(ClientItemResourcePack::modelLocation)
                        .map(ResourceLocation::getNamespace).ifPresent(namespaces::add);
            });
        }
        return namespaces;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        if (serializer == PackMetadataSection.TYPE || "pack".equals(serializer.getMetadataSectionName())) {
            return (T) new PackMetadataSection(Component.literal("Echo Item Pack dynamic assets"), PACK_FORMAT);
        }
        return null;
    }

    @Override
    public String packId() {
        return PACK_ID;
    }

    @Override
    public void close() {
    }

    public static String generatedModel(WeaponDefinition definition) {
        return "{\n"
                + "  \"parent\": \"minecraft:item/handheld\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": \"" + definition.visual().texture() + "\"\n"
                + "  }\n"
                + "}\n";
    }

    private Pack pack() {
        Pack.Info info = new Pack.Info(Component.literal("Echo Item Pack dynamic client assets"),
                PACK_FORMAT, FeatureFlags.DEFAULT_FLAGS);
        return Pack.create(PACK_ID, Component.literal("Echo Item Pack dynamic assets"), true, ignored -> this,
                info, PackType.CLIENT_RESOURCES, Pack.Position.TOP, true, PackSource.BUILT_IN);
    }

    private void listDefinition(String namespace, String path, ResourceOutput output, Set<ResourceLocation> emitted,
                                WeaponDefinition definition) {
        ResourceLocation model = modelLocation(definition.id());
        if (model.getNamespace().equals(namespace) && model.getPath().startsWith(path) && emitted.add(model)) {
            output.accept(model, () -> new ByteArrayInputStream(modelBytes(model).orElseThrow()));
        }
        textureLocation(definition.visual().texture()).filter(texture -> texture.getNamespace().equals(namespace))
                .filter(texture -> texture.getPath().startsWith(path))
                .filter(emitted::add)
                .flatMap(assets::texture)
                .ifPresent(bytes -> output.accept(textureLocation(definition.visual().texture()).orElseThrow(),
                        stream(bytes)));
    }

    private Optional<byte[]> modelBytes(ResourceLocation location) {
        Optional<byte[]> external = assets.model(location);
        if (external.isPresent()) {
            return external;
        }
        return catalog.snapshot().weapons().values().stream()
                .filter(definition -> modelLocation(definition.id()).equals(location))
                .findFirst()
                .map(this::itemModelBytes);
    }

    private byte[] itemModelBytes(WeaponDefinition definition) {
        return generatedModel(definition).getBytes(StandardCharsets.UTF_8);
    }

    private static String packMeta() {
        return "{\"pack\":{\"pack_format\":" + PACK_FORMAT
                + ",\"description\":\"Echo Item Pack dynamic assets\"}}\n";
    }

    private static IoSupplier<InputStream> stream(byte[] bytes) {
        return () -> new ByteArrayInputStream(bytes);
    }

    private static IoSupplier<InputStream> stream(String text) {
        return stream(text.getBytes(StandardCharsets.UTF_8));
    }

    private static ResourceLocation modelLocation(EchoItemId id) {
        return ResourceLocation.fromNamespaceAndPath(id.namespace(), "models/item/" + id.path() + ".json");
    }

    private static Optional<ResourceLocation> modelLocation(String modelRef) {
        return Optional.ofNullable(ResourceLocation.tryParse(modelRef))
                .map(ref -> ResourceLocation.fromNamespaceAndPath(ref.getNamespace(), "models/"
                        + ref.getPath() + ".json"));
    }

    private static Optional<ResourceLocation> textureLocation(String textureRef) {
        return Optional.ofNullable(ResourceLocation.tryParse(textureRef))
                .map(ref -> ResourceLocation.fromNamespaceAndPath(ref.getNamespace(), "textures/"
                        + ref.getPath() + ".png"));
    }
}
