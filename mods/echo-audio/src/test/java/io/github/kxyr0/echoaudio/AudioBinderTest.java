package io.github.kxyr0.echoaudio;

import io.github.kxyr0.echoaudio.api.*;
import io.github.kxyr0.echoaudio.script.AudioCommandBinder;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AudioBinderTest {
    AudioCue bind(String command) {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("audio.es"),
                "class Audio : StoryResource { private void Start() { " + command + " } }"));
        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        var method = (MethodSyntax) parsed.syntaxTree().orElseThrow().classDeclaration().members().get(0);
        var statement = (ExpressionStatementSyntax) method.body().statements().get(0);
        return new AudioCommandBinder().bind((InvocationExpressionSyntax) statement.expression());
    }
    @Test void simplestPlayNeedsNoOptionsAndRoundTripsPortablePayload() {
        var cue = bind("Sound.Play(\"jdh:door_knock\");");
        assertEquals(1, cue.options().volume()); assertEquals(1, cue.options().pitch());
        assertFalse(cue.loop()); assertFalse(cue.waitForEnd()); assertEquals(cue, AudioCue.decode(cue.payload()));
        assertEquals(cue, AudioCue.fromArguments(cue.arguments()));
    }
    @Test void namedLoopDurationFadePitchAndOwnerScopedNameAreParserValid() {
        var cue = bind("Sound.Loop(\"jdh:rain\", as: Rain, duration: Seconds(8), volume: 0.8, pitch: 0.95, fadeIn: Seconds(1), fadeOut: Seconds(2));");
        assertEquals("Rain", cue.name()); assertEquals(160, cue.options().durationTicks());
        assertEquals(20, cue.options().fadeInTicks()); assertEquals(40, cue.options().fadeOutTicks());
        assertTrue(cue.loop()); assertFalse(cue.waitForEnd());
    }
    @Test void spatialAndExplicitWaitAreDistinct() {
        assertEquals("BedroomDoor", bind("Sound.PlayAt(\"jdh:knock\", BedroomDoor);").location());
        assertEquals("BedroomDoor", bind("Sound.LoopAt(\"jdh:knock\", BedroomDoor, duration: Seconds(3));").location());
        assertTrue(bind("Sound.PlayAndWait(\"jdh:knock\");").waitForEnd());
        assertTrue(bind("Sound.LoopAndWait(\"jdh:knock\", duration: Seconds(3));").waitForEnd());
    }
    @Test void stopNameAndFadeAreParserValid() {
        var cue = bind("Sound.Stop(Rain, fadeOut: Seconds(2));");
        assertEquals("Rain", cue.name()); assertEquals(40, cue.options().fadeOutTicks());
    }
    @Test void invalidOptionsFailWithTypedCodes() {
        assertEquals("AUDIO_VOLUME_INVALID", assertThrows(AudioFailure.class,
                () -> bind("Sound.Play(\"jdh:knock\", volume: 2);")).code());
        assertEquals("AUDIO_PITCH_INVALID", assertThrows(AudioFailure.class,
                () -> bind("Sound.Play(\"jdh:knock\", pitch: 0.1);")).code());
        assertEquals("AUDIO_DURATION_INVALID", assertThrows(AudioFailure.class,
                () -> bind("Sound.LoopAndWait(\"jdh:knock\");")).code());
        var duplicate = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(new SourceId("audio.es"),
                "class A : StoryResource { private void Start() { Sound.Play(\"jdh:knock\", volume: 1, volume: 0.5); } }"));
        assertFalse(duplicate.successful());
        assertTrue(duplicate.diagnostics().stream().anyMatch(d -> d.code().equals("ESL-PARSE-010")));
        assertThrows(AudioFailure.class, () -> bind("Sound.Play(\"knock.wav\");"));
    }
}
