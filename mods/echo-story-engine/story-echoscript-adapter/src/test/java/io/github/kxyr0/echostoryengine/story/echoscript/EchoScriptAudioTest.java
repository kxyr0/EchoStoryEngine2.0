package io.github.kxyr0.echostoryengine.story.echoscript;

import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EchoScriptAudioTest {
    @Test void soundCommandsLowerToExternalActions() {
        var result = new EchoScriptStorySourceCompiler().compile(new StorySourceInput(
                "jdh:echo_story/stories/audio.es", """
                class AudioStory : StoryResource {
                    OnStart() {
                        Sound.Play("jdh:knock", volume: 0.7, pitch: 0.9);
                        Sound.Loop("jdh:rain", as: Rain, duration: Seconds(10));
                        Sound.Stop(Rain, fadeOut: Seconds(1));
                    }
                }
                """), Instant.EPOCH);
        assertTrue(result.compiledStory().isPresent(), () -> result.diagnostics().toString());
        assertEquals(3, result.compiledStory().orElseThrow().nodes().values().stream()
                .filter(n -> n.actionType().asString().equals("echo_audio:cue")).count());
    }
    @Test void invalidAudioProducesTypedSourceDiagnostic() {
        var result = new EchoScriptStorySourceCompiler().compile(new StorySourceInput(
                "jdh:echo_story/stories/audio.es", """
                class A : StoryResource { OnStart() { Sound.Play("jdh:knock", pitch: 5); } }
                """), Instant.EPOCH);
        assertTrue(result.compiledStory().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals("AUDIO_PITCH_INVALID")));
    }
}
