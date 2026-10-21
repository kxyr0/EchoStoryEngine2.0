package io.github.kxyr0.echocontentdoctor.source;

import io.github.kxyr0.echocontentdoctor.engine.ContentDoctor;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorInput;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.provider.ItemSnapshotProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.*;

class InspectionSourceCatalogTest {
    @TempDir Path root;

    @Test
    void sourceScannerDispatchesActualInspectionAndWeaponCompilersWithRegistrySnapshot() throws Exception {
        write("data/jdh/echo_item_inspection/inspections/letter.inspection.es", inspection("minecraft:paper", ""));
        write("data/jdh/echo_item_inspection/inspections/blade_view.inspection.es", inspection("jdh:blade", ""));
        write("data/jdh/echo_item/items/blade.weapon.es", weapon("damage: 3"));
        var scan = new SourceCatalogScanner().scan(root, new ItemSnapshotProvider(Set.of("minecraft:paper", "jdh:blade")));
        assertTrue(scan.diagnostics().isEmpty());
        assertEquals(3, scan.catalog().files());
        assertEquals(2, scan.catalog().resources().stream().filter(value -> value.kind() == ContentResourceKind.ITEM_INSPECTION).count());
        assertEquals(2, scan.catalog().resources().stream().filter(value -> value.kind() == ContentResourceKind.ITEM).count());
        assertTrue(new ContentDoctor().validate(ContentDoctorInput.catalogOnly(scan.catalog())).successful());
    }

    @Test
    void batchKeepsIndependentCompilerErrorsAndDoctorDuplicateAndTypoDiagnostics() throws Exception {
        write("data/jdh/echo_item_inspection/inspections/first.inspection.es", inspection("minecraft:paper", ""));
        write("data/jdh/echo_item_inspection/inspections/second.inspection.es", inspection("minecraft:paper", ""));
        write("data/jdh/echo_item_inspection/inspections/typo.inspection.es", inspection("jdh:father_leter", ""));
        write("data/jdh/echo_item_inspection/inspections/broken.inspection.es", inspection("minecraft:paper", "View(){ Scale(0); }"));
        var scan = new SourceCatalogScanner().scan(root,
                new ItemSnapshotProvider(Set.of("minecraft:paper", "jdh:father_letter")));
        var compilerError = scan.diagnostics().stream().filter(value -> value.code().equals("ITEM_INSPECTION_INVALID_VIEW"))
                .findFirst().orElseThrow();
        assertEquals("jdh:broken", compilerError.resource().value());
        assertEquals("data/jdh/echo_item_inspection/inspections/broken.inspection.es", compilerError.span().sourceId().value());
        var validation = new ContentDoctor().validate(ContentDoctorInput.catalogOnly(scan.catalog()));
        assertTrue(validation.diagnostics().stream().anyMatch(value -> value.code().equals("ITEM_INSPECTION_DUPLICATE_BINDING")));
        var missing = validation.diagnostics().stream().filter(value -> value.code().equals("ECHO_DOCTOR_REFERENCE_NOT_FOUND"))
                .findFirst().orElseThrow();
        assertEquals("jdh:father_letter", missing.suggestions().get(0).value());
        assertEquals(3, missing.span().start().line());
        assertEquals(4, scan.catalog().files());
    }

    @Test
    void sourceDefinitionPendingRegistryRegistrationCannotSatisfyInspectionReference() throws Exception {
        write("data/jdh/blade.weapon.es", weapon("damage: 3"));
        write("data/jdh/inspect.inspection.es", inspection("jdh:blade", ""));
        var scan = new SourceCatalogScanner().scan(root, new ItemSnapshotProvider(Set.of("minecraft:paper")));
        var result = new ContentDoctor().validate(ContentDoctorInput.catalogOnly(scan.catalog()));
        assertFalse(result.successful());
        assertTrue(result.diagnostics().stream().anyMatch(value -> value.message().contains("jdh:blade")));
    }

    @Test
    void malformedWeaponIsRejectedByRealCompilerInsteadOfBecomingAnItemDeclaration() throws Exception {
        write("data/jdh/blade.weapon.es", weapon("damage: -3"));
        var scan = new SourceCatalogScanner().scan(root);
        assertTrue(scan.catalog().resources().isEmpty());
        assertFalse(scan.diagnostics().isEmpty());
        assertTrue(scan.diagnostics().stream().anyMatch(value -> value.message().contains("damage")));
    }

    private String inspection(String item, String view) {
        return "class Test : ItemInspectionResource {\n\n Item(\"" + item + "\");\n" + view + "\n}";
    }

    private String weapon(String combat) {
        return "class Blade : WeaponResource { public override WeaponDefinition Build() { return Weapon("
                + "id: \"jdh:blade\", name: \"Blade\", texture: \"jdh:item/blade\", " + combat + "); } }";
    }

    private void write(String relative, String text) throws Exception {
        Path target = root.resolve(relative);
        Files.createDirectories(target.getParent());
        Files.writeString(target, text);
    }
}
