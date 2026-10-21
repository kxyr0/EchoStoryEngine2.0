package io.github.kxyr0.echostoryengine.story.forge.externalcontent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ExternalContentSeedExtractorTest {
    @TempDir
    Path temp;

    @Test
    void initializesMissingPackFromSeed() throws Exception {
        Path seed = seedWithStory("seed");
        Path target = temp.resolve("minecraft/story/content-pack/jdh");

        boolean created = new ExternalContentSeedExtractor().initialize(target, seed);

        assertTrue(created);
        assertEquals("seed", Files.readString(storyFile(target), StandardCharsets.UTF_8));
    }

    @Test
    void existingPackIsNeverOverwritten() throws Exception {
        Path seed = seedWithStory("seed");
        Path target = temp.resolve("minecraft/story/content-pack/jdh");
        write(target.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}");
        write(storyFile(target), "edited");

        boolean created = new ExternalContentSeedExtractor().initialize(target, seed);

        assertFalse(created);
        assertEquals("edited", Files.readString(storyFile(target), StandardCharsets.UTF_8));
    }

    @Test
    void existingPackReceivesNewSeedFilesWithoutOverwritingEdits() throws Exception {
        Path seed = seedWithStory("seed");
        write(animationFile(seed), "animation");
        Path target = temp.resolve("minecraft/story/content-pack/jdh");
        write(storyFile(target), "edited");

        boolean updated = new ExternalContentSeedExtractor().initialize(target, seed);

        assertTrue(updated);
        assertEquals("edited", Files.readString(storyFile(target), StandardCharsets.UTF_8));
        assertEquals("animation", Files.readString(animationFile(target), StandardCharsets.UTF_8));
    }

    private Path seedWithStory(String text) throws Exception {
        Path seed = temp.resolve("seed");
        write(seed.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"test\"}}");
        write(storyFile(seed), text);
        return seed;
    }

    private Path storyFile(Path root) {
        return root.resolve("data/jdh/echo_story/stories/chapter_1/home/home_arrival.es");
    }

    private Path animationFile(Path root) {
        return root.resolve("data/jdh/echo_animation/animations/chapter_1/family/wave.animation.es");
    }

    private void write(Path path, String text) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, text, StandardCharsets.UTF_8);
    }
}
