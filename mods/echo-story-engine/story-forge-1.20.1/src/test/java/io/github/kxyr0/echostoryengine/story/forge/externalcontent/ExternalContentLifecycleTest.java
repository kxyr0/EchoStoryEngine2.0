package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ExternalContentLifecycleTest {
    private static final byte[] PNG = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @TempDir
    Path temp;

    @Test
    void externalContentPackSupportsServerDataAndClientResources() {
        assertTrue(ExternalContentLifecycle.supportsExternalContentPack(PackType.SERVER_DATA));
        assertTrue(ExternalContentLifecycle.supportsExternalContentPack(PackType.CLIENT_RESOURCES));
    }

    @Test
    void externalContentPackServesClientAssetsFromStoryContentPack() throws Exception {
        SharedConstants.tryDetectVersion();
        Path root = temp.resolve("story/content-pack/jdh");
        writePackMeta(root);
        Path texture = root.resolve("assets/jdh/textures/echo_animation/faces/player/sclera_left.png");
        Files.createDirectories(texture.getParent());
        Files.write(texture, PNG);
        var packs = new ArrayList<net.minecraft.server.packs.repository.Pack>();

        new ExternalContentPackSource(root, PackType.CLIENT_RESOURCES).loadPacks(packs::add);

        assertEquals(1, packs.size());
        try (PackResources resources = packs.get(0).open()) {
            var supplier = resources.getResource(PackType.CLIENT_RESOURCES,
                    ResourceLocation.fromNamespaceAndPath("jdh",
                            "textures/echo_animation/faces/player/sclera_left.png"));
            assertNotNull(supplier);
            assertArrayEquals(PNG, supplier.get().readAllBytes());
        }
    }

    private static void writePackMeta(Path root) throws Exception {
        Files.createDirectories(root);
        Files.writeString(root.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}\n", StandardCharsets.UTF_8);
    }

    @Test
    void archivedPackIsHiddenForBothPackTypesAndCanBeRestored() throws Exception {
        SharedConstants.tryDetectVersion();
        Path root = temp.resolve("pack");
        writePackMeta(root);
        Path marker = root.resolve(ExternalContentPaths.ARCHIVE_MARKER);
        Files.writeString(marker, "archive");
        for (PackType type : PackType.values()) {
            var packs = new ArrayList<net.minecraft.server.packs.repository.Pack>();
            var source = new ExternalContentPackSource(root, type);
            source.loadPacks(packs::add);
            assertTrue(packs.isEmpty());
        }
        Files.delete(marker);
        var restored = new ArrayList<net.minecraft.server.packs.repository.Pack>();
        new ExternalContentPackSource(root, PackType.SERVER_DATA).loadPacks(restored::add);
        assertEquals(1, restored.size());
    }

    @Test
    void archivedPackDoesNotReceiveMissingSeedFiles() throws Exception {
        Path root = temp.resolve("pack");
        writePackMeta(root);
        Files.writeString(root.resolve(ExternalContentPaths.ARCHIVE_MARKER), "archive");
        Path seed = temp.resolve("seed");
        Files.createDirectories(seed);
        Files.writeString(seed.resolve("story.es"), "seed");
        assertEquals(false, new ExternalContentSeedExtractor().initialize(root, seed));
        assertEquals(false, Files.exists(root.resolve("story.es")));
        Files.delete(root.resolve(ExternalContentPaths.ARCHIVE_MARKER));
        assertTrue(new ExternalContentSeedExtractor().initialize(root, seed));
        assertTrue(Files.exists(root.resolve("story.es")));
    }
}
