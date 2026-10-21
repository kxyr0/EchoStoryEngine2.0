package io.github.kxyr0.echodebug.forge.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.kxyr0.echodebug")
class ForgeArchitectureTest {
    @ArchTest
    static final ArchRule DEBUG_DOES_NOT_IMPORT_TRIGGER_INTERNALS = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("io.github.kxyr0.echotrigger.core..");

    @ArchTest
    static final ArchRule DEBUG_DOES_NOT_IMPORT_ECHOSCRIPT = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("io.github.kxyr0.echoscript..");
}
