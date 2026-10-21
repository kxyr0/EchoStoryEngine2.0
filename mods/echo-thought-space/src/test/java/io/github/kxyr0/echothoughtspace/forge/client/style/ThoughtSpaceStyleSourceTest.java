package io.github.kxyr0.echothoughtspace.forge.client.style;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ThoughtSpaceStyleSourceTest {
    @TempDir Path directory;

    @Test
    void invalidHotReloadKeepsLastKnownGoodStyle() throws IOException {
        Path file = directory.resolve("jdh/echo_thought_space/ui.thought-space.es");
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                class Thoughts : ThoughtSpaceResource {
                    Style(circleRadius: 48, nodeFontSize: 1.5);
                    Thought First() { Text("First"); }
                    Combination Result() { From("a", "b"); Thought("Result"); }
                }
                """);
        ThoughtSpaceStyleSource source = new ThoughtSpaceStyleSource(directory);
        source.reloadNow();
        assertEquals(48.0D, source.current().circleRadius());
        assertEquals(1, source.declaredThoughts().nodes().size());
        assertEquals(1, source.combinations().size());
        long validRevision = source.revision();

        Files.writeString(file, "class Thoughts : ThoughtSpaceResource { Style(circleRadius: -1); }");
        Files.setLastModifiedTime(file, FileTime.fromMillis(System.currentTimeMillis() + 2_000L));
        source.reloadNow();
        assertEquals(48.0D, source.current().circleRadius());
        assertEquals(1, source.declaredThoughts().nodes().size());
        assertEquals(1, source.combinations().size());
        assertEquals(validRevision, source.revision());
    }
}
