package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DialogueAudioTest {
    private final EchoScriptDialogueCompiler compiler = new EchoScriptDialogueCompiler();

    @Test
    void userDialogueKeepsKnockBetweenSixthAndSeventhLines() throws Exception {
        String source;
        try (var stream = getClass().getResourceAsStream("/audio/after_dream.dialogue.es")) {
            source = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        var result = compiler.compile("jdh:after_dream.dialogue.es", source);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().nodes().stream()
                .flatMap(node -> node.commands().stream()).filter(c -> c.type().equals("echo_audio:cue")).toList();
        assertEquals(1, commands.size());
        assertEquals(6, commands.get(0).position());
        assertEquals("jdh:door_knock", commands.get(0).arguments().get("resource"));
    }

    @Test
    void allAudioOperationsCompileInAuthoredOrder() {
        var result = compile("""
                Sound.Play("jdh:knock", volume: 0.5);
                Sound.PlayAndWait("jdh:knock");
                Sound.PlayAt("jdh:knock", BedroomDoor);
                Sound.Loop("jdh:rain", as: Rain);
                Sound.LoopAt("jdh:rain", BedroomDoor, duration: Seconds(2));
                Sound.LoopAndWait("jdh:rain", duration: Seconds(2));
                Sound.Stop(Rain, fadeOut: Seconds(1));
                """);
        assertTrue(result.successful(), () -> result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().nodes().get(0).commands();
        assertEquals(7, commands.size());
        assertEquals("PlayAndWait", commands.get(1).arguments().get("operation"));
        assertEquals("Stop", commands.get(6).arguments().get("operation"));
    }

    @Test
    void invalidVolumeAndUnboundedAwaitAreDiagnosed() {
        assertCode("Sound.Play(\"jdh:knock\", volume: 2);", "AUDIO_VOLUME_INVALID");
        assertCode("Sound.LoopAndWait(\"jdh:rain\");", "AUDIO_DURATION_INVALID");
    }

    @Test
    void audioAfterTerminalIsRejected() {
        assertCode("Complete(\"jdh:done\"); Sound.Play(\"jdh:knock\");", "EDLG-AUDIO-AFTER-TERMINAL");
    }

    private DialogueCompileResult compile(String commands) {
        return compiler.compile("jdh:test.dialogue.es", """
                class Test : DialogueResource {
                    private void Init() { Id("jdh:test"); Start(Intro); Alias("john", "jdh:john"); }
                    private void Intro() { Line("john", "Привет");
                """ + commands + " } }");
    }

    private void assertCode(String source, String code) {
        var result = compile(source);
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(d -> d.code().equals(code)),
                () -> result.diagnostics().toString());
    }
}
