package io.github.kxyr0.echocontentdoctor.teleport;

import io.github.kxyr0.echocontentdoctor.source.SourceCatalogScanner;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class TeleportDoctorTest {
    @TempDir Path root;
    private void write(String path, String text) throws Exception {
        var file = root.resolve(path); Files.createDirectories(file.getParent()); Files.writeString(file, text);
    }
    @Test void sceneLocationTypoRetainsExactSourceLineAndRealCandidate() throws Exception {
        write("data/jdh/echo_scene/locations/chemistry_door.json", "{\"dimension\":\"minecraft:overworld\",\"x\":0,\"y\":64,\"z\":0}");
        write("data/jdh/echo_scene/scenes/teleport.es", """
                class Teleport : SceneResource {
                    Scene Start() {
                        TeleportMagic.To(John, ChemstryDoor);
                    }
                }
                """);
        var scan = new SourceCatalogScanner().scan(root);
        var missing = scan.diagnostics().stream().filter(d -> d.code().equals("SCENE_SYMBOL_NOT_FOUND")).findFirst().orElseThrow();
        assertEquals(3, missing.span().start().line());
        assertTrue(missing.message().contains("ChemistryDoor"));
    }
}
