package io.github.kxyr0.echonpcbackpack.script;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class NpcBackpackScriptBinderTest {
    @Test
    void documentedStoryExamplePassesRealParserAndBinder() throws IOException {
        Path example = Path.of("..", "docs", "examples", "molly-backpack-story.es");
        String source = Files.readString(example);
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("docs/examples/molly-backpack-story.es"), source));

        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        List<String> calls = new NpcBackpackScriptBinder().bind(parsed.syntaxTree().orElseThrow())
                .stream().map(NpcBackpackScriptBinding::canonicalName).toList();
        assertEquals(List.of(NpcBackpackScriptCalls.EQUIP, NpcBackpackScriptCalls.UNEQUIP), calls);
    }

    @Test
    void allCanonicalCallsBindWithTheirPublishedArity() {
        String source = """
                class Calls : StoryResource {
                    private void Run() {
                        Npc.EquipBackpack("jdh:molly", Position(1, 2, 3));
                        Npc.UnequipBackpack("jdh:molly", Position(4, 5, 6));
                        Npc.IsWearingBackpack("jdh:molly");
                        Npc.HasBackpackTransition("jdh:molly");
                        Npc.BackpackIsAt(Position(4, 5, 6));
                    }
                }
                """;
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("canonical-calls.es"), source));

        assertTrue(parsed.successful(), () -> parsed.diagnostics().toString());
        assertEquals(List.of(NpcBackpackScriptCalls.EQUIP, NpcBackpackScriptCalls.UNEQUIP,
                NpcBackpackScriptCalls.IS_WEARING, NpcBackpackScriptCalls.HAS_TRANSITION,
                NpcBackpackScriptCalls.IS_AT), new NpcBackpackScriptBinder()
                .bind(parsed.syntaxTree().orElseThrow()).stream()
                .map(NpcBackpackScriptBinding::canonicalName).toList());
    }
}
