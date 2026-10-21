package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionView;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InspectionCompilerTest {
    private final InspectionCompiler compiler = new InspectionCompiler();
    private final ItemInspectionDefinitionRevision revision = new ItemInspectionDefinitionRevision(9);

    @Test
    void minimalIntentUsesRealParserWithoutNamespaceOrBuildMethod() {
        var result = compile("Item(\"jdh:father_letter\");");
        assertTrue(result.successful(), result.diagnostics().toString());
        var definition = result.definition().orElseThrow();
        assertEquals("jdh:father_letter", definition.id().value());
        assertEquals("jdh:father_letter", definition.itemId());
        assertEquals(revision, definition.revision());
        assertEquals(ItemInspectionView.defaults(), definition.view());
    }

    @Test
    void namespaceAndClassNameDoNotReplaceStableSourceIdentity() {
        var source = new InspectionSource("data/jdh/echo_item_inspection/inspections/father_letter.inspection.es",
                "namespace Different.Content; class RenamedClass : ItemInspectionResource { Item(\"jdh:father_letter\"); }");
        var definition = compiler.compile(source, revision).definition().orElseThrow();
        assertEquals("jdh:father_letter", definition.id().value());
    }

    @Test
    void advancedViewBindsSignedLiteralsAndReorderedNamedArguments() {
        var definition = compile("""
                Item("jdh:father_letter");
                View() {
                    Scale(1.15);
                    Rotation(z: 0, y: -20, x: 12);
                    Zoom(initial: 1.0, max: 2.40, min: 0.65);
                }
                """).definition().orElseThrow();
        assertEquals(1.15, definition.view().scale());
        assertEquals(-20, definition.view().rotationY());
        assertEquals(2.4, definition.view().maxZoom());
    }

    @Test
    void missingItemReportsArgumentLocationAndNearbyKnownItem() {
        var source = source("\nItem(\"jdh:father_leter\");");
        var result = compiler.compile(source, revision, Set.of("jdh:father_letter", "minecraft:stone"));
        var diagnostic = result.diagnostics().get(0);
        assertFalse(result.successful());
        assertEquals("ITEM_INSPECTION_ITEM_NOT_FOUND", diagnostic.code());
        assertEquals(3, diagnostic.span().start().line());
        assertEquals("jdh:father_letter", diagnostic.suggestions().get(0));
        assertTrue(diagnostic.location().contains("father_letter.inspection.es:3:"));
    }

    @Test
    void offlineDescriptorIsTypedAndKeepsOriginalReferenceSpan() {
        var result = compiler.compile(source("Item(\"missing:offline\");"), revision);
        var descriptor = result.definition().orElseThrow().descriptor();
        assertEquals("missing:offline", descriptor.itemId());
        assertEquals(2, descriptor.itemSpan().start().line());
        assertEquals("data/jdh/echo_item_inspection/inspections/father_letter.inspection.es",
                descriptor.declarationSpan().sourceId().value());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "Item(\"jdh:father_letter\"); Item(\"jdh:father_letter\");",
            "Item(\"jdh:father_letter\"); Unknown();", "Item(Unknown);",
            "Item(\"missing_namespace\");", "Item(\"jdh:father_letter\"); View() {} View() {}",
            "Item(\"jdh:father_letter\"); View(1) {}", "Item(\"jdh:father_letter\"); View() { Pivot(1); }",
            "Item(\"jdh:father_letter\"); View() { Rotation(x: 1, nope: 2, z: 3); }",
            "Item(\"jdh:father_letter\"); View() { Scale(1); Scale(2); }",
            "Item(\"jdh:father_letter\"); View() { return; }",
            "Item(\"jdh:father_letter\"); View() { Scale(0); }",
            "Item(\"jdh:father_letter\"); View() { Scale(-1); }",
            "Item(\"jdh:father_letter\"); View() { Zoom(min: 2, max: 1, initial: 1); }",
            "Item(\"jdh:father_letter\"); View() { Zoom(min: 1, max: 2, initial: 3); }",
            "Item(\"jdh:father_letter\"); View() { Rotation(x: \"bad\", y: 0, z: 0); }",
            "Item(\"jdh:father_letter\")"
    })
    void invalidSyntaxOrSemanticsNeverProduceRuntimeDefinition(String body) {
        var result = compile(body);
        assertFalse(result.successful());
        assertTrue(result.definition().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.error()));
    }

    @Test
    void veryLargeRotationLiteralDoesNotBecomeParserIntegerFallbackZero() {
        var result = compile("Item(\"jdh:father_letter\"); View() { Rotation(3000000000, 0, 0); }");
        assertTrue(result.successful());
        assertEquals(3000000000d, result.definition().orElseThrow().view().rotationX());
    }

    @Test
    void nonfiniteNumericConversionIsRejected() {
        var result = compile("Item(\"jdh:father_letter\"); View() { Scale(" + "9".repeat(400) + ".0); }");
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("finite")));
    }

    @Test
    void canonicalSourceNamespaceIsRequiredAndMultipleResourcesAreRejected() {
        assertFalse(compiler.compile(new InspectionSource("father_letter.inspection.es",
                "class A:ItemInspectionResource { Item(\"jdh:father_letter\"); }"), revision).successful());
        assertFalse(compiler.compile(new InspectionSource("data/jdh/x.inspection.es",
                "class A:ItemInspectionResource{} class B:ItemInspectionResource{}"), revision).successful());
    }

    @Test
    void malformedClassThatThrowsInsideParserBecomesTypedSyntaxDiagnostic() {
        var source = new InspectionSource("data/jdh/letter.inspection.es", "class { invalid");
        var result = assertDoesNotThrow(() -> compiler.compile(source, revision));
        assertFalse(result.successful());
        var error = result.diagnostics().get(0);
        assertEquals("ITEM_INSPECTION_SYNTAX_ERROR", error.code());
        assertEquals(source.sourceId(), error.span().sourceId().value());
        assertEquals(1, error.span().start().line());
    }

    private InspectionCompileResult compile(String body) {
        return compiler.compile(source(body), revision, Set.of("jdh:father_letter"));
    }

    private InspectionSource source(String body) {
        return new InspectionSource("data/jdh/echo_item_inspection/inspections/father_letter.inspection.es",
                "class AuthoredName : ItemInspectionResource {\n" + body + "\n}");
    }
}
