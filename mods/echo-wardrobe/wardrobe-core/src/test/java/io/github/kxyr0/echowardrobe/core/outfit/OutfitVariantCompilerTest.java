package io.github.kxyr0.echowardrobe.core.outfit;

import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantDefinition;
import io.github.kxyr0.echowardrobe.api.outfit.OutfitVariantStatus;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class OutfitVariantCompilerTest {
    private final OutfitVariantCompiler compiler =
            new OutfitVariantCompiler(EchoScriptLanguage.parser());

    @Test
    void bindsExactOutfitVariantResource() {
        WardrobeResult<List<OutfitVariantDefinition>> result = compiler.compile("john_full_dressed.outfit.es",
                source(), "sha256:test", resource -> true);

        Assertions.assertTrue(result.successful());
        OutfitVariantDefinition variant = result.value().orElseThrow().get(0);
        Assertions.assertEquals("jdh:john/full_dressed", variant.id().value());
        Assertions.assertEquals("jdh:john", variant.profileId().value());
        Assertions.assertEquals("jdh:origin_john_glasses", variant.key().selection(WardrobeSlot.FACE)
                .wearableId().orElseThrow().value());
        Assertions.assertEquals("jdh:echo_wardrobe/skins/origin/jdh_full_dressed", variant.worldSkin());
        Assertions.assertEquals(OutfitVariantStatus.ACTIVE, variant.status());
    }

    @Test
    void skinCallSetsWorldAndFaceSkinTogether() {
        OutfitVariantDefinition variant = compiler.compile("john_full_dressed.outfit.es",
                source(), "sha256:test", resource -> true).value().orElseThrow().get(0);

        Assertions.assertEquals(variant.worldSkin(), variant.faceSkin());
    }

    @Test
    void missingSkinKeepsVariantButMarksItInactiveForExactLookup() {
        WardrobeResult<List<OutfitVariantDefinition>> result = compiler.compile("missing.outfit.es",
                source(), "sha256:test", resource -> false);

        Assertions.assertTrue(result.successful());
        Assertions.assertEquals(OutfitVariantStatus.MISSING_SKIN, result.value().orElseThrow().get(0).status());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_OUTFIT_SKIN_NOT_FOUND));
    }

    @Test
    void worldAndFaceSkinCanBeAuthoredSeparately() {
        String split = source().replace("Skin(\"jdh:echo_wardrobe/skins/origin/jdh_full_dressed\");",
                "WorldSkin(\"jdh:echo_wardrobe/skins/origin/jdh_full_dressed\");\n"
                        + "                        FaceSkin(\"jdh:echo_wardrobe/portraits/origin/jdh_full_dressed\");");

        OutfitVariantDefinition variant = compiler.compile("split.outfit.es", split, "sha256:test",
                resource -> true).value().orElseThrow().get(0);

        Assertions.assertEquals("jdh:echo_wardrobe/skins/origin/jdh_full_dressed", variant.worldSkin());
        Assertions.assertEquals("jdh:echo_wardrobe/portraits/origin/jdh_full_dressed", variant.faceSkin());
    }

    private String source() {
        return """
                namespace jdh.wardrobe.outfits.john.origin_robe;

                class JohnFullDressed : OutfitVariantResource
                {
                    private void Init()
                    {
                        Id("jdh:john/full_dressed");
                        Profile("jdh:john");
                        Match(
                            face: "jdh:origin_john_glasses",
                            top: "jdh:origin_john_robe",
                            bottom: "jdh:origin_john_pants",
                            feet: "jdh:origin_john_boots"
                        );
                        Skin("jdh:echo_wardrobe/skins/origin/jdh_full_dressed");
                    }
                }
                """;
    }
}
