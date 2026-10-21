package io.github.kxyr0.echonpc.echoscript;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import org.junit.jupiter.api.Test;

class EchoScriptNpcCompilerTest {
    private final EchoScriptNpcCompiler compiler = new EchoScriptNpcCompiler();
    private final EchoScriptNpcFormCompiler formCompiler = new EchoScriptNpcFormCompiler();

    @Test
    void validNpcResourceCompilesWithRussianDisplayAndSkin() {
        NpcCompileResult result = compiler.compile("sample:father.npc.es", fatherSource());
        var definition = result.definition().orElseThrow();
        assertEquals("sample:father", definition.id().value());
        assertEquals("Отец", definition.displayName());
        assertEquals("sample:father", definition.skinId().orElseThrow().value());
        assertEquals("#8FD8FF", definition.nameColor().orElseThrow().hex());
    }

    @Test
    void npcResourceAcceptsOpaqueRgbaNameColor() {
        NpcCompileResult result = compiler.compile("sample:father.npc.es",
                fatherSource().replace("#8FD8FF", "#9227cfff"));

        assertEquals("#9227CF", result.definition().orElseThrow().nameColor().orElseThrow().hex());
    }

    @Test
    void npcResourceWithoutSkinCompiles() {
        NpcCompileResult result = compiler.compile("sample:simple.npc.es", simpleSource());
        assertEquals("Житель", result.definition().orElseThrow().displayName());
        assertTrue(result.definition().orElseThrow().skinId().isEmpty());
    }

    @Test
    void invalidSourcesNeverProduceDefinitions() {
        assertRejected(fatherSource().replace("id: \"sample:father\",", ""));
        assertRejected(fatherSource().replace("Skin(\"sample:father\")", "Skin(\"bad id\")"));
        assertRejected(fatherSource().replace("nameColor(\"#8FD8FF\")", "nameColor(\"bad\")"));
        assertRejected(fatherSource().replace("Npc(", "Unknown("));
        assertRejected(fatherSource().replace("skin: Skin(\"sample:father\")", "animation: \"walk\""));
        assertRejected("class Broken : NpcResource {");
    }

    @Test
    void fingerprintIgnoresFormattingCommentsLineEndingsAndSourceId() {
        var first = compiler.compile("a", fatherSource()).definition().orElseThrow().fingerprint();
        String formatted = fatherSource().replace("return Npc(", "// Комментарий\n        return Npc(")
                .replace("\n", "\r\n");
        var second = compiler.compile("another:logical_source", formatted).definition().orElseThrow().fingerprint();
        assertEquals(first, second);
    }

    @Test
    void fingerprintChangesWhenDisplayOrSkinChangesButNotNameColor() {
        var base = compiler.compile("a", fatherSource()).definition().orElseThrow().fingerprint();
        var renamed = compiler.compile("b", fatherSource().replace("Отец", "Отец 2"))
                .definition().orElseThrow().fingerprint();
        var reskinned = compiler.compile("c", fatherSource().replace("sample:father\")", "sample:other\")"))
                .definition().orElseThrow().fingerprint();
        var recolored = compiler.compile("e", fatherSource().replace("#8FD8FF", "#FFCC00"))
                .definition().orElseThrow().fingerprint();
        var noSkin = compiler.compile("d", simpleSource().replace("sample:simple", "sample:father")
                .replace("Житель", "Отец")).definition().orElseThrow().fingerprint();
        assertNotEquals(base, renamed);
        assertNotEquals(base, reskinned);
        assertEquals(base, recolored);
        assertNotEquals(base, noSkin);
    }

    @Test
    void npcDefaultFormAndFormResourceCompileTogether() {
        NpcCompileResult npc = compiler.compile("jdh:sasha.npc.es", sashaNpcSource());
        NpcFormCompileResult form = formCompiler.compile("jdh:sasha/child.form.es", sashaChildFormSource(),
                new NpcFormRevision(3L));
        var definition = form.form().orElseThrow();

        assertEquals("jdh:sasha/child", npc.definition().orElseThrow().defaultFormId().orElseThrow().value());
        assertEquals("jdh:sasha/child", definition.id().value());
        assertEquals("jdh:npc/sasha/child", definition.appearance().skinId().orElseThrow().value());
        assertEquals(0.74D, definition.bodyGeometry().globalScale(), 0.0001D);
        assertEquals(1.22D, definition.dimensions().height(), 0.0001D);
        assertEquals(3L, definition.revision().value());
        assertTrue(definition.tags().contains("child"));
    }

    @Test
    void formResourceDefaultsSharedAnimationFaceAndPortrait() {
        NpcFormCompileResult form = formCompiler.compile("jdh:sasha/pajamas.form.es", sashaWardrobeFormSource(),
                new NpcFormRevision(4L));
        var definition = form.form().orElseThrow();

        assertEquals("jdh:sasha/pajamas", definition.id().value());
        assertEquals("jdh:npc/sasha/pajamas", definition.appearance().skinId().orElseThrow().value());
        assertEquals("echo_npc:humanoid_locomotion", definition.appearance().animationGraphId());
        assertEquals("echo_npc:humanoid_face", definition.appearance().faceProfileId());
        assertEquals("jdh:npc/sasha/pajamas", definition.appearance().portraitResourceId());
    }

    private void assertRejected(String source) {
        NpcCompileResult result = compiler.compile("sample:bad.npc.es", source);
        assertTrue(result.definition().isEmpty());
        assertTrue(result.diagnostics().stream()
                .anyMatch(io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic::error));
    }

    private String fatherSource() {
        return """
                namespace EchoNpc.Sample;

                class FatherNpc : NpcResource
                {
                    public override NpcResourceDef Build()
                    {
                        return Npc(
                            id: "sample:father",
                            display: "Отец",
                            skin: Skin("sample:father"),
                            nameColor: nameColor("#8FD8FF")
                        );
                    }
                }
                """;
    }

    private String simpleSource() {
        return """
                namespace EchoNpc.Sample;

                class SimpleNpc : NpcResource
                {
                    public override NpcResourceDef Build()
                    {
                        return Npc(
                            id: "sample:simple",
                            display: "Житель"
                        );
                    }
                }
                """;
    }

    private String sashaNpcSource() {
        return """
                namespace JDH.Npcs.Sasha;

                class SashaNpc : NpcResource
                {
                    public override NpcResourceDef Build()
                    {
                        return Npc(
                            id: "jdh:sasha",
                            display: "Саша",
                            skin: Skin("jdh:npc/sasha/adult"),
                            defaultForm: DefaultForm("jdh:sasha/child")
                        );
                    }
                }
                """;
    }

    private String sashaChildFormSource() {
        return """
                namespace JDH.Npcs.Sasha.Forms;

                class SashaChildForm : NpcFormResource
                {
                    public override NpcFormDef Build()
                    {
                        return Form(
                            id: "jdh:sasha/child",
                            skin: Skin("jdh:npc/sasha/child"),
                            model: "echo_npc:humanoid",
                            dimensions: Dimensions(width: 0.42, height: 1.22, eyeHeight: 1.06),
                            bodyScale: 0.74,
                            parts: [Part(id: "head", scale: 1.18)],
                            animationGraph: "jdh:sasha/child_locomotion",
                            faceProfile: "jdh:sasha/child_face",
                            portrait: "jdh:sasha/child",
                            tags: ["sasha", "child"]
                        );
                    }
                }
                """;
    }

    private String sashaWardrobeFormSource() {
        return """
                namespace JDH.Npcs.Sasha.Forms;

                class SashaPajamasForm : NpcFormResource
                {
                    public override NpcFormDef Build()
                    {
                        return Form(
                            id: "jdh:sasha/pajamas",
                            skin: Skin("jdh:npc/sasha/pajamas"),
                            model: "echo_npc:humanoid",
                            dimensions: Dimensions(width: 0.60, height: 1.80, eyeHeight: 1.62),
                            bodyScale: 1.0,
                            tags: ["sasha", "adult", "pajamas"]
                        );
                    }
                }
                """;
    }
}
