package io.github.kxyr0.echovisibility.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.core.catalog.VisibilityWatchCatalogState;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VisibilityResourceLoaderTest {
    @Test
    void reparsesOnlyChangedSourceAndKeepsLastGood(@TempDir Path directory) throws Exception {
        Path watches = directory.resolve("jdh/echo_visibility/watches");
        Files.createDirectories(watches);
        Path father = watches.resolve("father.es");
        Path mother = watches.resolve("mother.es");
        Files.writeString(father, source("jdh:father_watch", "jdh:father", 1));
        Files.writeString(mother, source("jdh:mother_watch", "jdh:mother", 1));
        VisibilityResourceLoader loader = new VisibilityResourceLoader();
        VisibilityWatchCatalogState catalog = new VisibilityWatchCatalogState();

        assertEquals(2, loader.reload(directory, catalog).parserCalls());
        Files.writeString(father, source("jdh:father_watch", "jdh:father", 3));
        var incremental = loader.reload(directory, catalog);
        assertEquals(1, incremental.parserCalls());
        assertEquals(1, incremental.binderCalls());
        Files.writeString(father, "broken source");
        assertEquals(1, loader.reload(directory, catalog).parserCalls());
        assertTrue(catalog.find(new VisibilityWatchId("jdh:father_watch")).isPresent());
    }

    private static String source(String watch, String npc, int edge) {
        return """
                class Watch : VisibilityWatchResource
                {
                    private void Init()
                    {
                        Id("%s");
                        Target(Npc("%s"));
                        HiddenWhen(OUTSIDE_FRUSTUM);
                        Occlusion(NONE);
                        EdgeMargin(Pixels(%d));
                    }
                }
                """.formatted(watch, npc, edge);
    }
}
