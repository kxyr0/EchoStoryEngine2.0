package io.github.kxyr0.echodialoguetriggeradapter;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureRulesTest {
    private final JavaClasses classes = new ClassFileImporter()
            .importPackages("io.github.kxyr0.echodialoguetriggeradapter");

    @Test
    void adapterUsesOnlyPublicDialogueAndTriggerApis() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "io.github.kxyr0.echodialogue.core..",
                "io.github.kxyr0.echodialogue.forge.host..",
                "io.github.kxyr0.echotrigger.core..",
                "io.github.kxyr0.echotrigger.forge.service..")
                .check(classes);
    }
}
