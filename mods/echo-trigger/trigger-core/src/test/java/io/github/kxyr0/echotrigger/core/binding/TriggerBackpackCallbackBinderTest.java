package io.github.kxyr0.echotrigger.core.binding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotrigger.api.callback.TriggerCallbackKind;
import io.github.kxyr0.echotrigger.api.id.TriggerRevision;
import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import org.junit.jupiter.api.Test;

final class TriggerBackpackCallbackBinderTest {
    @Test
    void canonicalBackpackCallKeepsPositionPayload() {
        String source = """
                class BackpackTrigger : TriggerResource
                {
                    private TriggerArea Area = CircleArea(Vector3(0, 64, 0), 2);
                    private void Init()
                    {
                        Id("jdh:backpack_trigger");
                        Dimension("minecraft:overworld");
                        Bind(Area);
                    }
                    private void OnTriggerEnter(PlayerRef player)
                    {
                        Npc.EquipBackpack("jdh:molly", Position(37, -3, 100));
                    }
                }
                """;
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId("backpack.trigger.es"), source));
        assertTrue(parsed.successful(), parsed.diagnostics().toString());
        var result = new TriggerDefinitionBinder().bind(
                parsed.syntaxTree().orElseThrow(), "backpack", new TriggerRevision(1));

        assertTrue(result.successful(), result.diagnostics().toString());
        var action = result.definition().orElseThrow().reactive().orElseThrow()
                .callbacks().get(TriggerCallbackKind.ENTER).actions().get(0);
        assertEquals("Npc.EquipBackpack", action.name());
        assertEquals("jdh:molly", action.arguments().get("arg0"));
        assertEquals("Position(37,-3,100)", action.arguments().get("arg1"));
    }
}
