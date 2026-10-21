package io.github.kxyr0.echoitem.forge.resource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClientItemAssetStoreTest {
    private static final byte[] PNG_ONE = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1
    };

    @TempDir
    Path tempDir;

    @Test
    void brokenTextureKeepsPreviousGoodAndDeletionRevealsFallback() throws IOException {
        ClientItemAssetStore store = new ClientItemAssetStore(tempDir);
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("jdh", "textures/item/void_sword.png");
        Path source = tempDir.resolve("story/item-pack/jdh/assets/textures/item/void_sword.png");
        Files.createDirectories(source.getParent());

        Files.write(source, PNG_ONE);
        assertArrayEquals(PNG_ONE, store.texture(texture).orElseThrow());

        Files.write(source, new byte[] {1, 2, 3});
        assertArrayEquals(PNG_ONE, store.texture(texture).orElseThrow());

        Files.delete(source);
        assertTrue(store.texture(texture).isEmpty());
    }

    @Test
    void brokenModelKeepsPreviousGoodAndDeletionRevealsFallback() throws IOException {
        ClientItemAssetStore store = new ClientItemAssetStore(tempDir);
        ResourceLocation model = ResourceLocation.fromNamespaceAndPath("jdh", "models/item/void_sword.json");
        Path source = tempDir.resolve("story/item-pack/jdh/assets/models/item/void_sword.json");
        Files.createDirectories(source.getParent());
        byte[] valid = "{\"parent\":\"minecraft:item/handheld\"}".getBytes(StandardCharsets.UTF_8);

        Files.write(source, valid);
        assertArrayEquals(valid, store.model(model).orElseThrow());

        Files.writeString(source, "{broken", StandardCharsets.UTF_8);
        assertArrayEquals(valid, store.model(model).orElseThrow());

        Files.delete(source);
        assertTrue(store.model(model).isEmpty());
    }

    @Test
    void resourcesListsAllValidExternalClientAssets() throws IOException {
        ClientItemAssetStore store = new ClientItemAssetStore(tempDir);
        Path texture = tempDir.resolve("story/item-pack/jdh/assets/textures/item/custom_3d.png");
        Path model = tempDir.resolve("story/item-pack/jdh/assets/models/item/custom_3d.json");
        Files.createDirectories(texture.getParent());
        Files.createDirectories(model.getParent());
        Files.write(texture, PNG_ONE);
        Files.writeString(model, "{\"parent\":\"minecraft:item/generated\"}", StandardCharsets.UTF_8);

        Map<ResourceLocation, byte[]> resources = store.resources("jdh", "");

        assertTrue(resources.containsKey(ResourceLocation.fromNamespaceAndPath("jdh",
                "textures/item/custom_3d.png")));
        assertTrue(resources.containsKey(ResourceLocation.fromNamespaceAndPath("jdh",
                "models/item/custom_3d.json")));
    }
}
