package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.compile.pipeline.SceneCompiler;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import io.github.kxyr0.echoscene.api.descriptor.SceneReferenceKind;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SceneAudioTest {
    @Test void sharedSoundDslCompilesWithExistingLocationAndAudioReferences() {
        var compiler = new SceneCompiler(SceneSymbolCatalog.of(List.of(
                new SceneSymbol("BedroomDoor", "jdh:bedroom_door", SceneSymbolKind.LOCATION))));
        var result = compiler.compile(new SceneSourceInput("jdh", "jdh/echo_scene/scenes/knock.es", """
                class Knock : SceneResource {
                    Scene Start() {
                        Sound.Play("jdh:knock");
                        Sound.LoopAt("jdh:knock", BedroomDoor, as: Knocking, duration: Seconds(6), volume: 0.8);
                        Sound.Stop(Knocking, fadeOut: Seconds(1));
                    }
                }
                """, 1));
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertTrue(result.descriptor().orElseThrow().references().stream().anyMatch(r -> r.kind() == SceneReferenceKind.AUDIO));
        assertTrue(result.descriptor().orElseThrow().references().stream().anyMatch(r -> r.target().equals("jdh:bedroom_door")));
    }
    @Test void missingLocationAndInvalidVolumeAreSourceDiagnostics() {
        var compiler = new SceneCompiler(SceneSymbolCatalog.of(List.of()));
        for (String command : List.of("Sound.PlayAt(\"jdh:knock\", Missing);", "Sound.Play(\"jdh:knock\", volume: 4);")) {
            var result = compiler.compile(new SceneSourceInput("jdh", "jdh/echo_scene/scenes/knock.es",
                    "class Knock : SceneResource { Scene Start() { " + command + " } }", 1));
            assertFalse(result.successful()); assertTrue(result.diagnostics().get(0).code().startsWith("AUDIO_"));
        }
    }
}
