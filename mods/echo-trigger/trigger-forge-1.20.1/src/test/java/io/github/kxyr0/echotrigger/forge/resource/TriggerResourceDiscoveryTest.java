package io.github.kxyr0.echotrigger.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TriggerResourceDiscoveryTest {
    private final TriggerResourceDiscovery discovery = new TriggerResourceDiscovery();

    @Test
    void discoversCanonicalAndContentPackTriggerRoots(@TempDir Path gameDirectory) throws Exception {
        TriggerContentRoots roots = new TriggerContentRoots(gameDirectory);
        Path canonical = roots.canonicalRoot().resolve("first.trigger.es");
        Path packed = roots.contentPackRoot().resolve("jdh/data/jdh/echo_trigger/triggers/chapter/packed.trigger.es");
        Path legacy = roots.contentPackRoot().resolve("legacy/echo_trigger/triggers/legacy.trigger.es");
        Path unrelated = roots.contentPackRoot().resolve("jdh/data/jdh/echo_seating/zones/ignored.trigger.es");
        write(canonical);
        write(packed);
        write(legacy);
        write(unrelated);

        List<Path> paths = discovery.externalTriggers(roots).stream()
                .map(TriggerResourceText::path).toList();

        assertEquals(List.of(canonical, packed, legacy).stream().sorted().toList(), paths);
    }

    private static void write(Path path) throws Exception {
        Files.createDirectories(path.getParent());
        Files.writeString(path, "class Test : TriggerResource {}");
    }
}
