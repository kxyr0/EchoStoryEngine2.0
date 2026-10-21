package io.github.kxyr0.echodebug.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.kxyr0.echodebug.core")
class CoreArchitectureTest {
    @ArchTest
    static final ArchRule CORE_DOES_NOT_IMPORT_MINECRAFT = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..");

    @ArchTest
    static final ArchRule CORE_DOES_NOT_IMPORT_TRIGGER_INTERNALS = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("io.github.kxyr0.echotrigger.core..");
}
