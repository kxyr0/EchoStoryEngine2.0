package io.github.kxyr0.echoitem.forge.resource;

import io.github.kxyr0.echoitem.api.EchoItemId;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.api.definition.WeaponDefinition;
import io.github.kxyr0.echoitem.api.source.ItemSourceFingerprint;
import io.github.kxyr0.echoitem.api.text.ItemTextDefinition;
import io.github.kxyr0.echoitem.api.visual.ItemVisualDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponCombatDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponDurabilityDefinition;
import io.github.kxyr0.echoitem.api.weapon.WeaponRarity;
import io.github.kxyr0.echoitem.core.catalog.ItemCatalogSnapshot;
import io.github.kxyr0.echoitem.core.catalog.WeaponDefinitionCatalog;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientItemResourcePackTest {
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @TempDir
    Path tempDir;

    @Test
    void generatedModelUsesHandheldParentAndTextureLayer() {
        var definition = WeaponDefinitionCatalog.unavailable(EchoItemId.parse("jdh:void_sword"),
                new ItemDefinitionRevision(1L));

        String json = ClientItemResourcePack.generatedModel(definition);

        assertTrue(json.contains("minecraft:item/handheld"));
        assertTrue(json.contains(definition.visual().texture()));
    }

    @Test
    void packServesIconModelForCatalogItemWithSeparateHandModel() throws IOException {
        EchoItemId id = EchoItemId.parse("jdh:custom_sword");
        var definition = weaponWithHandModel(id);
        WeaponDefinitionCatalog catalog = new WeaponDefinitionCatalog();
        catalog.publish(ItemCatalogSnapshot.empty().withWeapons(List.of(definition)));
        var pack = new ClientItemResourcePack(catalog, new ClientItemAssetStore(tempDir));

        var supplier = pack.getResource(PackType.CLIENT_RESOURCES,
                ResourceLocation.fromNamespaceAndPath("jdh", "models/item/custom_sword.json"));

        assertNotNull(supplier);
        String json = new String(supplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"layer0\": \"jdh:item/custom_sword\""));
        assertFalse(json.contains("custom_sword_3d"));
    }

    @Test
    void packServesGeneratedModelForCatalogItem() throws IOException {
        EchoItemId id = EchoItemId.parse("jdh:void_sword");
        var definition = WeaponDefinitionCatalog.unavailable(id, new ItemDefinitionRevision(1L));
        WeaponDefinitionCatalog catalog = new WeaponDefinitionCatalog();
        catalog.publish(ItemCatalogSnapshot.empty().withWeapons(List.of(definition)));
        var pack = new ClientItemResourcePack(catalog, new ClientItemAssetStore(tempDir));

        var supplier = pack.getResource(PackType.CLIENT_RESOURCES,
                ResourceLocation.fromNamespaceAndPath("jdh", "models/item/void_sword.json"));

        assertNotNull(supplier);
        String json = new String(supplier.get().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(json.contains("minecraft:item/handheld"));
        assertTrue(json.contains(definition.visual().texture()));
    }

    @Test
    void packServesValidExternalTexture() throws IOException {
        var pack = new ClientItemResourcePack(new WeaponDefinitionCatalog(), new ClientItemAssetStore(tempDir));
        Path source = tempDir.resolve("story/item-pack/jdh/assets/textures/item/void_sword.png");
        Files.createDirectories(source.getParent());
        Files.write(source, PNG);

        var supplier = pack.getResource(PackType.CLIENT_RESOURCES,
                ResourceLocation.fromNamespaceAndPath("jdh", "textures/item/void_sword.png"));

        assertNotNull(supplier);
        assertArrayEquals(PNG, supplier.get().readAllBytes());
    }

    @Test
    void packAnnouncesExternalAssetNamespacesUsedByAuthoredModels() throws IOException {
        var pack = new ClientItemResourcePack(new WeaponDefinitionCatalog(), new ClientItemAssetStore(tempDir));
        Path texture = tempDir.resolve(
                "story/item-pack/echostoryengine/assets/textures/item/traveler/jdh_glasses_texture.png");
        Files.createDirectories(texture.getParent());
        Files.write(texture, PNG);

        assertTrue(pack.getNamespaces(PackType.CLIENT_RESOURCES).contains("echostoryengine"));
    }

    private static WeaponDefinition weaponWithHandModel(EchoItemId id) {
        return new WeaponDefinition(id,
                new ItemTextDefinition("Custom Sword", "", List.of()),
                new ItemVisualDefinition("jdh:item/custom_sword", Optional.of("jdh:item/custom_sword_3d"),
                        true, 0L),
                new WeaponCombatDefinition(7.0D, 1.6D, 0.0D),
                new WeaponDurabilityDefinition(250, 0, Optional.empty(), false),
                WeaponRarity.COMMON,
                false,
                false,
                Optional.empty(),
                false,
                OptionalInt.empty(),
                new ItemDefinitionRevision(1L),
                new ItemSourceFingerprint("a".repeat(64)),
                true);
    }
}
