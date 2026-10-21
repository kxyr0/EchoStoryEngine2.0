package io.github.kxyr0.echoobservation.echoscript;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservationAudioTest {
    @Test void commonSoundDslWorksDirectlyInsideOnComplete() {
        var result = compile("""
                Sound.Play("jdh:knock", volume: 0.7);
                Sound.LoopAt("jdh:rain", BedroomDoor, as: Rain, duration: Seconds(3));
                Sound.Stop(Rain, fadeOut: Seconds(1));
                """);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var actions = result.resource().orElseThrow().observations().get(0).onComplete().actions();
        assertEquals(3, actions.size()); assertEquals("echo_audio:cue", actions.get(0).target());
    }
    @Test void completionCannotLeakInfiniteLoopsOrPretendToAwaitAudio() {
        assertFalse(compile("Sound.Loop(\"jdh:rain\");").successful());
        assertFalse(compile("Sound.PlayAndWait(\"jdh:knock\");").successful());
        assertFalse(compile("Sound.Play(\"jdh:knock\", volume: 4);").successful());
    }
    private ObservationCompileResult compile(String calls) {
        return new ObservationResourceCompiler().compile("jdh:echo_observation/observations/audio.es",
                "class A : ObservationResource { Observation Door() { OnComplete() { " + calls + " } } }");
    }
}
