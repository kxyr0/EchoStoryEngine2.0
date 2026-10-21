package io.github.kxyr0.echostoryanimationadapter.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import org.junit.jupiter.api.Test;

final class ArchitectureRulesTest {
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.kxyr0.echostoryanimationadapter");

    @Test
    void adapterDoesNotImportStoryOrAnimationImplementations() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echostoryengine.story.core..",
                        "io.github.kxyr0.echostoryengine.story.definition..",
                        "io.github.kxyr0.echostoryengine.story.persistence..",
                        "io.github.kxyr0.echostoryengine.story.forge..",
                        "io.github.kxyr0.echoanimation.core..",
                        "io.github.kxyr0.echoanimation.definition..",
                        "io.github.kxyr0.echoanimation.echoscript..",
                        "io.github.kxyr0.echoanimation.forge.playback..",
                        "io.github.kxyr0.echoanimation.forge.resource..",
                        "io.github.kxyr0.echoanimation.forge.client..",
                        "io.github.kxyr0.echoanimation.forge.network..",
                        "io.github.kxyr0.echonpc..",
                        "io.github.kxyr0.echodialogue..")
                .check(classes);
    }

    @Test
    void adapterDoesNotUseFilesReflectionOrBackgroundExecutors() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echoscript..",
                        "java.io..",
                        "java.nio.file..",
                        "java.lang.reflect..",
                        "java.util.ServiceLoader",
                        "java.util.concurrent.Executor..")
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
