package io.github.kxyr0.echocontentdoctor.cli;

import io.github.kxyr0.echocontentdoctor.provider.ItemSnapshotProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class ItemCatalogCliTest {
    @TempDir Path temporary;

    @Test
    void actualCliAcceptsExplicitRegistryCatalogAndWritesPassingReport() throws Exception {
        Path root = content("minecraft:paper");
        Path registry = registry("# Client registry keys\nminecraft:paper\njdh:father_letter\n");
        Path report = temporary.resolve("report.json");
        assertEquals(0, ContentDoctorCli.run(new String[]{"--resources", root.toString(),
                "--item-catalog", registry.toString(), report.toString()}));
        assertTrue(Files.readString(report).contains("\"errors\": 0"));
    }

    @Test
    void unavailableRegistryIsExplicitConfigurationErrorInsteadOfMissingVanillaItem() throws Exception {
        Path root = content("minecraft:paper");
        var error = assertThrows(IllegalStateException.class, () -> ContentDoctorCli.run(new String[]{
                "--resources", root.toString(), temporary.resolve("report.json").toString()}));
        assertTrue(error.getMessage().startsWith("ITEM_INSPECTION_ITEM_CATALOG_REQUIRED"));
    }

    @Test
    void actualCliReportsMissingItemWithSourceLineAndRegistryTypoSuggestion() throws Exception {
        Path root = content("jdh:father_leter");
        Path registry = registry("minecraft:paper\njdh:father_letter\n");
        Path report = temporary.resolve("report.json");
        assertEquals(1, ContentDoctorCli.run(new String[]{"--resources", root.toString(),
                "--item-catalog", registry.toString(), report.toString()}));
        String json = Files.readString(report);
        assertTrue(json.contains("\"suggestions\":[\"jdh:father_letter\"]"));
        assertTrue(json.contains("\"line\":3"));
        assertTrue(json.contains("letter.inspection.es"));
    }

    @Test
    void malformedOrEmptyRegistryHasPreciseReadFailureAndCannotSupplyPartialKeys() throws Exception {
        var invalid = assertThrows(IOException.class,
                () -> ItemSnapshotProvider.read(registry("minecraft:paper\nMinecraft:Stone\n")));
        assertTrue(invalid.getMessage().contains(":2:"));
        assertThrows(IOException.class, () -> ItemSnapshotProvider.read(registry("# no keys\n")));
    }

    private Path content(String item) throws Exception {
        Path root = temporary.resolve("content");
        Path source = root.resolve("data/jdh/echo_item_inspection/inspections/letter.inspection.es");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "class Letter : ItemInspectionResource {\n\n Item(\"" + item + "\");\n}");
        return root;
    }

    private Path registry(String text) throws Exception {
        Path path = temporary.resolve("items.txt");
        Files.writeString(path, text);
        return path;
    }
}
