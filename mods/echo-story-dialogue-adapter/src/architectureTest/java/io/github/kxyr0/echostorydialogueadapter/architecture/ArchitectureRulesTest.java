package io.github.kxyr0.echostorydialogueadapter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureRulesTest {
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.kxyr0.echostorydialogueadapter");

    @Test
    void adapterDoesNotImportStoryOrDialogueImplementations() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echostoryengine.story.core..",
                        "io.github.kxyr0.echostoryengine.story.definition..",
                        "io.github.kxyr0.echostoryengine.story.persistence..",
                        "io.github.kxyr0.echostoryengine.story.forge..",
                        "io.github.kxyr0.echodialogue.core..",
                        "io.github.kxyr0.echodialogue.persistence..",
                        "io.github.kxyr0.echodialogue.echoscript..",
                        "io.github.kxyr0.echodialogue.forge.client..",
                        "io.github.kxyr0.echodialogue.forge.host..",
                        "io.github.kxyr0.echodialogue.forge.network..")
                .check(classes);
    }

    @Test
    void adapterDoesNotImportEchoScriptOrPersistenceFacilities() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echoscript..",
                        "java.io..",
                        "java.nio.file..",
                        "java.lang.reflect..")
                .check(classes);
    }

    @Test
    void adapterDoesNotUseServiceLoaderOrExecutors() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "java.util.ServiceLoader",
                        "java.util.concurrent..")
                .check(classes);
    }

    @Test
    void productionCodeHasNoMutableStaticFields() {
        List<String> mutableStaticFields = classes.stream()
                .flatMap(javaClass -> javaClass.getFields().stream())
                .filter(this::isMutableStatic)
                .map(JavaField::getFullName)
                .toList();

        assertTrue(mutableStaticFields.isEmpty(), "Mutable static fields: " + mutableStaticFields);
    }

    private boolean isMutableStatic(JavaField field) {
        return field.getModifiers().contains(JavaModifier.STATIC)
                && !field.getModifiers().contains(JavaModifier.FINAL);
    }
}
