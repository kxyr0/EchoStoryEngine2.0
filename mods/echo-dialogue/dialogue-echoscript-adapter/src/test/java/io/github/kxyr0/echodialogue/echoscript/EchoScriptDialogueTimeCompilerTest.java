package io.github.kxyr0.echodialogue.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class EchoScriptDialogueTimeCompilerTest {
    @Test
    void canonicalTimeDslCompilesToDurableExternalCommands() {
        String source = """
                namespace JDH.Chapter1.Home;

                class LateDialogue : DialogueResource
                {
                    private void Init()
                    {
                        Id("jdh:late_dialogue");
                        Start(Greeting);
                        Alias("john", "jdh:john");
                    }

                    private void Greeting()
                    {
                        Line("john", "Late.");
                        Time.Set(Day(2), "08:30");
                        Time.Add(Minutes(5));
                        Time.Pause();
                        Time.Resume();
                        Time.WaitUntil("10:00");
                        Time.IsBetween("23:00", "01:00");
                        Line("john", "Now.");
                        Complete("jdh:done");
                    }
                }
                """;

        DialogueCompileResult result = new EchoScriptDialogueCompiler().compile(
                "jdh/late.dialogue.es", source);

        assertTrue(result.successful(), result.diagnostics().toString());
        var commands = result.dialogue().orElseThrow().startNode().commands();
        assertEquals(6, commands.size());
        assertEquals("echo_time:set", commands.get(0).type());
        assertEquals("2", commands.get(0).arguments().get("day"));
        assertEquals("echo_time:add", commands.get(1).type());
        assertEquals("Minutes", commands.get(1).arguments().get("durationUnit"));
        assertEquals("echo_time:wait_until", commands.get(4).type());
        assertEquals("23:00", commands.get(5).arguments().get("start"));
    }
}
