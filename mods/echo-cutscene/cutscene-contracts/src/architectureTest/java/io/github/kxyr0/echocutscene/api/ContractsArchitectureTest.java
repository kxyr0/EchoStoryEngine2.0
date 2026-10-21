package io.github.kxyr0.echocutscene.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ContractsArchitectureTest {
    @Test
    void contractsDoNotImportMinecraftOrForge() {
        var classes = new ClassFileImporter().importPackages("io.github.kxyr0.echocutscene.api");

        noClasses().should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..")
                .check(classes);
    }
}
