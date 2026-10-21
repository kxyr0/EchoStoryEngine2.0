package io.github.kxyr0.echoiteminspection.catalog;

import io.github.kxyr0.echoiteminspection.compile.InspectionSource;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InspectionCatalogTest {
    private final InspectionCatalog catalog = new InspectionCatalog();
    private final Set<String> items = Set.of("minecraft:paper", "minecraft:stone");

    @Test
    void validReloadPublishesAtomicRevisionAndRemovedDefinitionsDisappear() {
        var first = catalog.reload(List.of(source("letter", "minecraft:paper", "")), items);
        assertTrue(first.applied());
        assertEquals(1, catalog.snapshot().revision().value());
        assertTrue(catalog.resolve("minecraft:paper").isPresent());
        assertThrows(UnsupportedOperationException.class, () -> first.snapshot().definitions().clear());
        assertTrue(catalog.reload(List.of(), items).applied());
        assertEquals(2, catalog.snapshot().revision().value());
        assertTrue(catalog.resolve("minecraft:paper").isEmpty());
    }

    @Test
    void brokenSyntaxKeepsEntireLastKnownGoodCatalogIncludingDeletedCandidate() {
        catalog.reload(List.of(source("letter", "minecraft:paper", "")), items);
        var lastGood = catalog.snapshot();
        var broken = new InspectionSource("data/jdh/broken.inspection.es", "class Broken :");
        var result = catalog.reload(List.of(broken), items);
        assertFalse(result.applied());
        assertSame(lastGood, catalog.snapshot());
        assertTrue(catalog.resolve("minecraft:paper").isPresent());
    }

    @Test
    void parserExceptionCannotEscapeReloadOrReplaceLastKnownGoodCatalog() {
        catalog.reload(List.of(source("letter", "minecraft:paper", "")), items);
        var lastGood = catalog.snapshot();
        var malformed = new InspectionSource("data/jdh/broken.inspection.es", "class { invalid");
        var result = assertDoesNotThrow(() -> catalog.reload(List.of(malformed), items));
        assertFalse(result.applied());
        assertSame(lastGood, catalog.snapshot());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_SYNTAX_ERROR")));
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_LAST_KNOWN_GOOD_RETAINED")));
    }

    @Test
    void duplicateItemBindingsRejectBatchWithRequiredStableDiagnosticCode() {
        var result = catalog.reload(List.of(source("a", "minecraft:paper", ""), source("b", "minecraft:paper", "")), items);
        assertFalse(result.applied());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_DUPLICATE_BINDING")));
        assertTrue(catalog.snapshot().definitions().isEmpty());
    }

    @Test
    void duplicateInferredIdsAcrossFoldersAreRejectedEvenForDistinctItems() {
        var result = catalog.reload(List.of(source("a/shared", "minecraft:paper", ""),
                source("b/shared", "minecraft:stone", "")), items);
        assertFalse(result.applied());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_DUPLICATE_RESOURCE")));
    }

    @Test
    void pinnedDefinitionKeepsOldViewWhileNextOpenUsesNewRevision() {
        catalog.reload(List.of(source("letter", "minecraft:paper", "View(){ Zoom(0.65, 2, 1); }")), items);
        var pinned = catalog.resolve("minecraft:paper").orElseThrow();
        catalog.reload(List.of(source("letter", "minecraft:paper", "View(){ Zoom(0.65, 3, 1); }")), items);
        var latest = catalog.resolve("minecraft:paper").orElseThrow();
        assertEquals(2, pinned.view().maxZoom());
        assertEquals(3, latest.view().maxZoom());
        assertTrue(latest.revision().value() > pinned.revision().value());
    }

    @Test
    void registryUnknownItemAndInvalidViewDoNotPartiallyPublishValidSibling() {
        var result = catalog.reload(List.of(source("a", "minecraft:paper", ""), source("b", "jdh:absent", "")), items);
        assertFalse(result.applied());
        assertTrue(catalog.snapshot().definitions().isEmpty());
        var invalid = catalog.reload(List.of(source("a", "minecraft:paper", "View(){ Zoom(0, 2, 1); }")), items);
        assertFalse(invalid.applied());
        assertEquals(0, catalog.snapshot().revision().value());
    }

    @Test
    void textReloadPinsOpenDefinitionWhileNewSessionsReceiveLatestAuthoredText() {
        catalog.reload(List.of(source("letter", "minecraft:paper", "Title(\"Старый заголовок\"); Description(\"Старый текст\");")), items);
        var pinned = catalog.resolve("minecraft:paper").orElseThrow();
        catalog.reload(List.of(source("letter", "minecraft:paper", "Title(\"Новый заголовок\"); Description(\"Новый текст\");")), items);
        var latest = catalog.resolve("minecraft:paper").orElseThrow();
        assertEquals("Старый заголовок", pinned.text().title());
        assertEquals("Старый текст", pinned.text().description());
        assertEquals("Новый заголовок", latest.text().title());
        assertEquals("Новый текст", latest.descriptor().text().description());
        assertTrue(latest.revision().value() > pinned.revision().value());
    }

    @Test
    void invalidTextReloadRetainsLastKnownGoodTextAndRevision() {
        catalog.reload(List.of(source("letter", "minecraft:paper", "Title(\"Сохранённый\"); Description(\"Текст\");")), items);
        var lastGood = catalog.snapshot();
        var result = catalog.reload(List.of(source("letter", "minecraft:paper", "Title(123);")), items);
        assertFalse(result.applied());
        assertSame(lastGood, catalog.snapshot());
        assertEquals("Сохранённый", catalog.resolve("minecraft:paper").orElseThrow().text().title());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_INVALID_TEXT")));
    }

    private InspectionSource source(String name, String item, String extra) {
        return new InspectionSource("data/jdh/echo_item_inspection/inspections/" + name + ".inspection.es",
                "class Resource:ItemInspectionResource{ Item(\"" + item + "\"); " + extra + " }");
    }
}
