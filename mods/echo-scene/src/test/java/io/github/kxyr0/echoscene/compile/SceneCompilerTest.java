package io.github.kxyr0.echoscene.compile;

import io.github.kxyr0.echoscene.api.definition.SceneCapabilityKind;
import io.github.kxyr0.echoscene.compile.pipeline.SceneCompiler;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SceneCompilerTest {
    private final SceneCompiler compiler = new SceneCompiler(SceneSymbolCatalog.of(List.of(
            symbol("Father", "jdh:father", SceneSymbolKind.NPC),
            symbol("John", "jdh:john", SceneSymbolKind.PLAYER),
            symbol("KitchenTable", "jdh:kitchen_table", SceneSymbolKind.LOCATION),
            symbol("Chair", "jdh:chair", SceneSymbolKind.LOCATION),
            symbol("KitchenWide", "jdh:kitchen_wide", SceneSymbolKind.CAMERA),
            symbol("MorningDialogue", "jdh:morning_with_father", SceneSymbolKind.DIALOGUE))));

    @Test
    void compilesCanonicalHighLevelSceneWithInferredIdentityAndPlan() {
        SceneCompileResult result = compiler.compile(source("morning_kitchen.es", """
                class MorningKitchen : SceneResource
                {
                    Scene Start()
                    {
                        Time.Set("08:20");
                        Parallel()
                        {
                            Father.MoveTo(KitchenTable);
                            John.MoveTo(Chair);
                        }
                        Camera.Use(KitchenWide);
                        Dialogue.Play("jdh:morning_with_father");
                    }
                }
                """));

        assertTrue(result.successful(), () -> result.diagnostics().toString());
        assertEquals("jdh:morning_kitchen", result.definition().orElseThrow().resourceId().value());
        assertEquals("start", result.definition().orElseThrow().entry().value());
        assertEquals(List.of(1, 2, 1, 1), result.definition().orElseThrow().stages().stream()
                .map(stage -> stage.commands().size()).toList());
        assertEquals(List.of(SceneCapabilityKind.PLAYER, SceneCapabilityKind.CAMERA, SceneCapabilityKind.NPC),
                result.definition().orElseThrow().capabilityPlan().stream().map(value -> value.kind()).toList());
    }

    @Test
    void multipleScenesRequireExactlyOneStartModifier() {
        SceneCompileResult valid = compiler.compile(source("day.es", """
                class Day : SceneResource {
                    start Scene Morning() { Time.Set("08:00"); }
                    Scene Evening() { Time.Set("19:00"); }
                }
                """));
        SceneCompileResult invalid = compiler.compile(source("day.es", """
                class Day : SceneResource {
                    Scene Morning() { Time.Set("08:00"); }
                    Scene Evening() { Time.Set("19:00"); }
                }
                """));

        assertTrue(valid.successful(), () -> valid.diagnostics().toString());
        assertEquals("morning", valid.definition().orElseThrow().entry().value());
        assertTrue(invalid.diagnostics().stream().anyMatch(value -> value.code().equals("SCENE_ENTRY_MISSING")));
    }

    @Test
    void unknownSymbolsAreSemanticErrorsWithSourceSpan() {
        SceneCompileResult result = compiler.compile(source("broken.es", """
                class Broken : SceneResource { Scene Start() { Father.MoveTo(MissingRoom); } }
                """));

        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("SCENE_SYMBOL_NOT_FOUND")));
        assertEquals(1, result.diagnostics().get(0).span().start().line());
    }

    private SceneSourceInput source(String path, String text) {
        return new SceneSourceInput("jdh", path, text, 1);
    }

    private static SceneSymbol symbol(String name, String id, SceneSymbolKind kind) {
        return new SceneSymbol(name, id, kind);
    }
}
