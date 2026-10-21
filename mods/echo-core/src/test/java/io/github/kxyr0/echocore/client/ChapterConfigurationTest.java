package io.github.kxyr0.echocore.client;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChapterConfigurationTest {
    @TempDir Path game;
    @Test void defaultsToUserTemplateAndAddsChaptersWithoutNewCode() throws Exception {
        var chapters = ChapterConfiguration.load(game);
        assertEquals(game.resolve("saves/world"), chapters.get("chapter_1").template());
        Files.writeString(game.resolve("config/echo-core/chapters.properties"),
                "chapter_1.template=saves/world\nchapter_2.template=templates/second\nchapter_3.template=templates/third\n");
        chapters = ChapterConfiguration.load(game);
        assertEquals(3, chapters.size());
        assertEquals("echo_core_chapter_2", chapters.get("chapter_2").saveId());
    }
    @Test void rejectsUnknownKeysAndBlankPaths() throws Exception {
        ChapterConfiguration.load(game);
        Path config = game.resolve("config/echo-core/chapters.properties");
        Files.writeString(config, "chapter_1.save=../../world\n");
        assertThrows(java.io.IOException.class, () -> ChapterConfiguration.load(game));
        Files.writeString(config, "chapter_1.template=\n");
        assertThrows(java.io.IOException.class, () -> ChapterConfiguration.load(game));
    }
}
