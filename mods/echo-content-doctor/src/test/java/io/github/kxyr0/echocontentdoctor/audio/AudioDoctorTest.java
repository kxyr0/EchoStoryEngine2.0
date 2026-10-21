package io.github.kxyr0.echocontentdoctor.audio;

import io.github.kxyr0.echocontentdoctor.source.SourceCatalogScanner;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraphBuilder;
import io.github.kxyr0.echocontentdoctor.validation.reference.ReferenceValidator;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class AudioDoctorTest {
    @TempDir Path root;
    private void write(String path, String text) throws Exception {
        Path target = root.resolve(path); Files.createDirectories(target.getParent()); Files.writeString(target, text);
    }
    @Test void missingStorySoundSuggestsRealAssetFromSharedScanner() throws Exception {
        write("jdh/sounds/door_knock.wav", "test descriptor only");
        write("data/jdh/echo_story/stories/audio.es", """
                class AudioStory : StoryResource {
                    OnStart() { Sound.Play("jdh:door_knok"); }
                }
                """);
        var scan = new SourceCatalogScanner().scan(root);
        assertTrue(scan.diagnostics().isEmpty(), () -> scan.diagnostics().toString());
        var diagnostics = new ReferenceValidator().validate(new ContentGraphBuilder().build(scan.catalog()));
        var missing = diagnostics.stream().filter(d -> d.code().equals("AUDIO_ASSET_NOT_FOUND")).findFirst().orElseThrow();
        assertTrue(missing.suggestions().stream().anyMatch(id -> id.value().equals("jdh:door_knock")));
    }
    @Test void duplicateExtensionsAreErrorsAndDoNotSatisfyReference() throws Exception {
        write("jdh/sounds/knock.wav", "a"); write("jdh/sounds/knock.mp3", "b");
        var scan = new SourceCatalogScanner().scan(root);
        assertTrue(scan.diagnostics().stream().anyMatch(d -> d.code().equals("AUDIO_ASSET_AMBIGUOUS")));
        assertTrue(scan.catalog().resources().stream().noneMatch(r -> r.id().value().equals("jdh:knock")));
    }
    @Test void invalidSoundParametersComeFromHostBinder() throws Exception {
        write("data/jdh/echo_story/stories/audio.es", """
                class AudioStory : StoryResource {
                    OnStart() {
                        Sound.Play("jdh:knock", volume: 8);
                    }
                }
                """);
        var scan = new SourceCatalogScanner().scan(root);
        assertTrue(scan.diagnostics().stream().anyMatch(d -> d.code().equals("AUDIO_VOLUME_INVALID")));
        assertEquals(3, scan.diagnostics().stream().filter(d -> d.code().equals("AUDIO_VOLUME_INVALID"))
                .findFirst().orElseThrow().span().start().line());
    }
    @Test void observationCompletionPublishesAudioReferencesThroughSharedInspection() throws Exception {
        write("data/jdh/echo_observation/observations/audio.es", """
                class A : ObservationResource {
                    Observation Door() { OnComplete() { Sound.Play("jdh:missing"); } }
                }
                """);
        var scan = new SourceCatalogScanner().scan(root);
        var diagnostics = new ReferenceValidator().validate(new ContentGraphBuilder().build(scan.catalog()));
        assertTrue(diagnostics.stream().anyMatch(d -> d.code().equals("AUDIO_ASSET_NOT_FOUND")));
    }
}
