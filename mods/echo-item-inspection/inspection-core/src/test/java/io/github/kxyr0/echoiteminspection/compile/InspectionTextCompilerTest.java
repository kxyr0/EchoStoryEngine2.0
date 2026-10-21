package io.github.kxyr0.echoiteminspection.compile;

import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinition;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDescriptor;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionText;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class InspectionTextCompilerTest {
    private final InspectionCompiler compiler = new InspectionCompiler();
    private final ItemInspectionDefinitionRevision revision = new ItemInspectionDefinitionRevision(4);

    @Test
    void oldMinimalResourceStillCompilesWithEmptyTextDefaults() {
        var definition = compile("").definition().orElseThrow();
        assertEquals(ItemInspectionText.defaults(), definition.text());
        assertEquals(definition.text(), definition.descriptor().text());
    }

    @Test
    void literalUnicodeAndEscapedParagraphsQuotesAndBackslashesRemainExact() {
        var result = compile("""
                Title("Письмо отца — 記憶 ✉");
                Description("Первая строка\\n\\nВторая: \\"цитата\\" и путь C:\\\\письма");
                """);
        assertTrue(result.successful(), result.diagnostics().toString());
        var text = result.definition().orElseThrow().text();
        assertEquals("Письмо отца — 記憶 ✉", text.title());
        assertEquals("Первая строка\n\nВторая: \"цитата\" и путь C:\\письма", text.description());
    }

    @Test
    void titleAndDescriptionAreIndependentAndExplicitEmptyStringsAreAllowed() {
        assertEquals(new ItemInspectionText("Заголовок", ""), compile("Title(\"Заголовок\");")
                .definition().orElseThrow().text());
        assertEquals(new ItemInspectionText("", "Только описание"), compile("Description(\"Только описание\");")
                .definition().orElseThrow().text());
        assertEquals(ItemInspectionText.defaults(), compile("Title(\"\"); Description(\"\");")
                .definition().orElseThrow().text());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Title(123);", "Description(false);", "Title(Name());", "Description([\"text\"]);",
            "Title(\"first\"); Title(\"second\");", "Description(\"first\"); Description(\"second\");",
            "Title();", "Description(\"one\", \"two\");", "Title(wrong: \"text\");",
            "View(){ Title(\"not a View setting\"); }"
    })
    void duplicateWrongTypeOrWrongPositionTextIsRejected(String declarations) {
        var result = compile(declarations);
        assertFalse(result.successful());
        assertTrue(result.definition().isEmpty());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.error()));
    }

    @Test
    void limitsCountUnicodeCodepointsAndAcceptExactBoundaries() {
        String title = "🗝".repeat(ItemInspectionText.MAX_TITLE_LENGTH);
        String description = "я".repeat(ItemInspectionText.MAX_DESCRIPTION_LENGTH);
        var result = compile("Title(\"" + title + "\"); Description(\"" + description + "\");");
        assertTrue(result.successful(), result.diagnostics().toString());
        assertEquals(title, result.definition().orElseThrow().text().title());
        assertEquals(description, result.definition().orElseThrow().text().description());
    }

    @Test
    void overLimitTextFailsWithTypedLocatedDiagnostic() {
        for (String declaration : new String[]{"Title(\"" + "x".repeat(257) + "\");",
                "Description(\"" + "x".repeat(8193) + "\");"}) {
            var result = compile(declaration);
            assertFalse(result.successful());
            var diagnostic = result.diagnostics().get(0);
            assertEquals("ITEM_INSPECTION_INVALID_TEXT", diagnostic.code());
            assertEquals(3, diagnostic.span().start().line());
        }
    }

    @Test
    void programmaticTextValidatesLimitsAndOldPublicConstructorsRemainCompatible() {
        var definition = compile("").definition().orElseThrow();
        var oldDescriptor = new ItemInspectionDescriptor(definition.id(), definition.itemId(), definition.view(),
                definition.descriptor().declarationSpan(), definition.descriptor().itemSpan());
        var oldDefinition = new ItemInspectionDefinition(definition.id(), definition.itemId(), revision,
                definition.view(), oldDescriptor);
        assertEquals(ItemInspectionText.defaults(), oldDefinition.text());
        var withText = new ItemInspectionDescriptor(definition.id(), definition.itemId(), definition.view(),
                oldDescriptor.declarationSpan(), oldDescriptor.itemSpan(), new ItemInspectionText("Title", "Body"));
        assertEquals(withText.text(), new ItemInspectionDefinition(definition.id(), definition.itemId(), revision,
                definition.view(), withText).text());
        assertThrows(IllegalArgumentException.class, () -> new ItemInspectionText("x".repeat(257), ""));
        assertThrows(IllegalArgumentException.class, () -> new ItemInspectionText("", "x".repeat(8193)));
    }

    private InspectionCompileResult compile(String declarations) {
        return compiler.compile(new InspectionSource("data/jdh/echo_item_inspection/inspections/letter.inspection.es",
                "class Letter : ItemInspectionResource {\n Item(\"minecraft:paper\");\n" + declarations + "\n}"),
                revision, Set.of("minecraft:paper"));
    }
}
