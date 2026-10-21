package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echocontentdoctor.graph.ContentCatalogSnapshot;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.source.adapter.inspection.InspectionSourceAdapter;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemInspectionIntegrationTest {
    private final InspectionSourceAdapter adapter = new InspectionSourceAdapter();

    @Test
    void consumesActualCompilerDescriptorWithOptionalNamespaceAndItemReference() {
        var adapted = resource("letter", "minecraft:paper", "");
        assertTrue(adapted.problems().isEmpty());
        var inspection = adapted.descriptors().get(0);
        assertEquals(ContentResourceKind.ITEM_INSPECTION, inspection.kind());
        assertEquals("jdh:letter", inspection.id().value());
        assertEquals("minecraft:paper", inspection.references().get(0).target().value());
        assertTrue(validate(List.of(inspection, item(inspection, "minecraft:paper"))).successful());
    }

    @Test
    void missingItemProducesSourceLocationAndTypoSuggestion() {
        var inspection = resource("letter", "jdh:father_leter", "").descriptors().get(0);
        var result = validate(List.of(inspection, item(inspection, "jdh:father_letter")));
        var error = result.diagnostics().stream().filter(value ->
                value.code().equals("ECHO_DOCTOR_REFERENCE_NOT_FOUND")).findFirst().orElseThrow();
        assertEquals("jdh:father_letter", error.suggestions().get(0).value());
        assertTrue(error.span().start().line() > 0);
    }

    @Test
    void duplicateBindingIsRejectedByDoctor() {
        var first = resource("first", "minecraft:paper", "").descriptors().get(0);
        var second = resource("second", "minecraft:paper", "").descriptors().get(0);
        assertTrue(validate(List.of(first, second, item(first, "minecraft:paper"))).diagnostics().stream()
                .anyMatch(value -> value.code().equals("ITEM_INSPECTION_DUPLICATE_BINDING")));
    }

    @Test
    void invalidScaleZoomRotationAndMissingItemFailAtCompilation() {
        for (String view : List.of("View(){ Scale(0); }", "View(){ Zoom(min:2,max:1,initial:1); }",
                "View(){ Rotation(x:1,y:2,z:\"invalid\"); }")) {
            assertFalse(resource("letter", "minecraft:paper", view).problems().isEmpty());
        }
        var result = adapter.adapt("data/jdh/echo_item_inspection/inspections/letter.inspection.es",
                "class Letter : ItemInspectionResource { }");
        assertFalse(result.problems().isEmpty());
    }

    @Test
    void syntaxErrorAndMissingInspectionReferenceAreReported() {
        assertFalse(adapter.adapt("data/jdh/letter.inspection.es", "class { invalid").problems().isEmpty());
        var inspection = resource("letter", "minecraft:paper", "").descriptors().get(0);
        var reference = new io.github.kxyr0.echocontentdoctor.model.ContentReference(
                io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind.ITEM_INSPECTION,
                new ContentResourceId("jdh:missing_inspection"), inspection.span());
        var story = ContentDescriptor.simple(new ContentResourceId("jdh:test"), ContentResourceKind.STORY,
                inspection.span(), List.of(reference));
        assertFalse(validate(List.of(story)).successful());
    }

    @Test
    void authoredTitleAndMultilineDescriptionUseTheSameSemanticCompiler() {
        var adapted = resource("letter", "minecraft:paper",
                "Title(\"Тестовый предмет\"); Description(\"Первая строка.\\nВторая строка.\");");
        assertTrue(adapted.problems().isEmpty());
        var inspection = adapted.descriptors().get(0);
        assertTrue(validate(List.of(inspection, item(inspection, "minecraft:paper"))).successful());
    }

    @Test
    void malformedOrRepeatedAuthoredTextIsReportedAsSourceProblems() {
        for (String text : List.of("Title(17);", "Description(false);",
                "Title(\"A\"); Title(\"B\");", "Description(\"A\"); Description(\"B\");")) {
            assertFalse(resource("letter", "minecraft:paper", text).problems().isEmpty(), text);
        }
    }

    private io.github.kxyr0.echocontentdoctor.source.SourceAdapter.AdaptedSource resource(
            String name, String item, String view) {
        return adapter.adapt("data/jdh/echo_item_inspection/inspections/" + name + ".inspection.es",
                "class Letter : ItemInspectionResource { Item(\"" + item + "\"); " + view + " }");
    }

    private ContentDescriptor item(ContentDescriptor source, String id) {
        return ContentDescriptor.simple(new ContentResourceId(id), ContentResourceKind.ITEM, source.span(), List.of());
    }

    private ContentDoctorResult validate(List<ContentDescriptor> descriptors) {
        return new ContentDoctor().validate(ContentDoctorInput.catalogOnly(
                new ContentCatalogSnapshot(1, 1, descriptors)));
    }
}
