package io.github.kxyr0.echowardrobe.core.binding;

import io.github.kxyr0.echowardrobe.api.definition.WearableDefinition;
import io.github.kxyr0.echowardrobe.api.definition.WearableDefinitionStatus;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeDiagnosticCode;
import io.github.kxyr0.echowardrobe.api.diagnostic.WardrobeResult;
import io.github.kxyr0.echowardrobe.api.slot.WardrobeSlot;
import io.github.kxyr0.echowardrobe.core.equipment.ItemRegistryView;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class WearableDefinitionCompilerTest {
    private final WearableDefinitionCompiler compiler =
            new WearableDefinitionCompiler(EchoScriptLanguage.parser());

    @Test
    void bindsFlatWearableResource() {
        WardrobeResult<List<WearableDefinition>> result = compiler.compile("origin_john_glasses.wearable.es",
                glassesSource(), "sha256:test", ItemRegistryView.allKnown());

        Assertions.assertTrue(result.successful());
        WearableDefinition definition = result.value().orElseThrow().get(0);
        Assertions.assertEquals("jdh:origin_john_glasses", definition.id().value());
        Assertions.assertEquals("jdh:john", definition.profileId().value());
        Assertions.assertEquals(WardrobeSlot.FACE, definition.slot());
        Assertions.assertEquals("origin", definition.collection());
        Assertions.assertTrue(definition.model().isPresent());
        Assertions.assertTrue(definition.portraitOverlay().isPresent());
        Assertions.assertEquals(-0.5D,
                definition.portraitOverlay().orElseThrow().strips().get(0).pixelYOffset());
        Assertions.assertEquals(0.8D,
                definition.portraitOverlay().orElseThrow().strips().get(0).pixelLeftInset());
        Assertions.assertEquals(0.8D,
                definition.portraitOverlay().orElseThrow().strips().get(0).pixelRightInset());
        Assertions.assertEquals(0.9D,
                definition.portraitOverlay().orElseThrow().strips().get(0).targetHeightScale());
        Assertions.assertEquals(WearableDefinitionStatus.ACTIVE, definition.status());
    }

    @Test
    void unknownItemStaysBoundButInactive() {
        WardrobeResult<List<WearableDefinition>> result = compiler.compile("origin_john_glasses.wearable.es",
                glassesSource(), "sha256:test", ItemRegistryView.noneKnown());

        Assertions.assertTrue(result.successful());
        Assertions.assertEquals(WearableDefinitionStatus.UNRESOLVED_ITEM,
                result.value().orElseThrow().get(0).status());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_ITEM_UNRESOLVED));
    }

    @Test
    void unsupportedLegacySkinCallsAreRejected() {
        String source = glassesSource().replace("ModelScale(1, 1, 1);",
                "ModelScale(1, 1, 1);\n                        LegacySkinLayer(\"jdh:echo_wardrobe/legacy/pants\");");

        WardrobeResult<List<WearableDefinition>> result = compiler.compile("legacy.wearable.es",
                source, "sha256:test", ItemRegistryView.allKnown());

        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code() == WardrobeDiagnosticCode.WARDROBE_WEARABLE_INVALID));
    }

    @Test
    void equipmentMismatchIsInvalid() {
        String source = glassesSource().replace("EquipmentSlot(HEAD)", "EquipmentSlot(CHEST)");

        WardrobeResult<List<WearableDefinition>> result = compiler.compile("bad.wearable.es",
                source, "sha256:test", ItemRegistryView.allKnown());

        Assertions.assertFalse(result.successful());
        Assertions.assertTrue(result.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code()
                        == WardrobeDiagnosticCode.WARDROBE_EQUIPMENT_SLOT_MISMATCH));
    }

    private String glassesSource() {
        return """
                namespace jdh.wardrobe.wearables.john.origin;

                class OriginJohnGlasses : WearableResource
                {
                    private void Init()
                    {
                        Id("jdh:origin_john_glasses");
                        Profile("jdh:john");
                        Item("jdh:origin_john_glasses");
                        Slot(FACE);
                        EquipmentSlot(HEAD);
                        Collection(origin);
                        Model("jdh:item/origin_john_glasses");
                        ModelPosition(0, 0, 0);
                        ModelRotation(0, 0, 0);
                        ModelScale(1, 1, 1);
                        PortraitTexture("jdh:textures/echo_wardrobe/skins/origin/jdh_full_dressed.png");
                        PortraitStrip(40, 12, 0, 4, 8, 1, OUTER, 0, -0.5, 0.8, 0.8, 0.9);
                        PortraitStrip(40, 13, 0, 5, 8, 1, OUTER, 0, 0);
                    }
                }
                """;
    }
}
