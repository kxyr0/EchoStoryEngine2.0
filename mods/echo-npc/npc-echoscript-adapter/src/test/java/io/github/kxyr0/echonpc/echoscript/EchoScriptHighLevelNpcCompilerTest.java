package io.github.kxyr0.echonpc.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EchoScriptHighLevelNpcCompilerTest {
    private final EchoScriptNpcCompiler compiler = new EchoScriptNpcCompiler();

    @Test
    void infersIdentityAndPreservesLegacyDefinition() {
        String source = """
                class FatherNpc : NpcResource {
                    Display("Отец");
                    Skin("sample:father");
                    NameColor("#8FD8FF");
                }
                """;
        String oldSource = """
                class FatherNpc : NpcResource {
                    public override NpcResourceDef Build() {
                        return Npc(id: "sample:father", display: "Отец", skin: Skin("sample:father"),
                            nameColor: nameColor("#8FD8FF"));
                    }
                }
                """;
        var modern = compiler.compile("sample:echo_npc/npcs/father.npc.es", source).definition().orElseThrow();
        var legacy = compiler.compile("sample:father.npc.es", oldSource).definition().orElseThrow();
        assertEquals(legacy, modern);
        assertEquals(legacy, compiler.compile("sample:father.npc.es", source).definition().orElseThrow());
        assertEquals("sample:sasha/child", compiler.compile("sample:father.npc.es",
                source.replace("Display(", "DefaultForm(\"sample:sasha/child\"); Display("))
                .definition().orElseThrow().defaultFormId().orElseThrow().value());
    }

    @Test
    void rejectsDuplicatesUnknownCallsAndMixedProfiles() {
        assertRejected("class N : NpcResource { Id(\"sample:n\"); Display(\"N\"); Display(\"Other\"); }");
        assertRejected("class N : NpcResource { Id(\"sample:n\"); Display(\"N\"); Unknown(\"x\"); }");
        assertRejected("class N : NpcResource { Id(\"sample:n\"); Display(3); }");
        assertRejected("class N : NpcResource { Id(\"sample:n\"); Display(\"N\"); Skin(\"bad id\"); }");
        assertRejected("class N : NpcResource { Display(\"N\"); void Build() { } }");
        assertRejected("class N : NpcResource { Display(\"N\"); At(\"place\") { } }");
        assertRejected("class N : OtherResource { Display(\"N\"); }");
    }

    private void assertRejected(String source) {
        NpcCompileResult result = compiler.compile("sample:n.npc.es", source);
        assertTrue(result.definition().isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic::error));
    }
}
