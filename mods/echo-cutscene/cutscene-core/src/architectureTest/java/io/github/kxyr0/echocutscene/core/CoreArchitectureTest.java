package io.github.kxyr0.echocutscene.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class CoreArchitectureTest {
    @Test
    void coreDoesNotImportMinecraftForgeOrDialogue() {
        var classes = new ClassFileImporter().importPackages("io.github.kxyr0.echocutscene.core");

        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "net.minecraft..",
                "net.minecraftforge..",
                "io.github.kxyr0.echodialogue.."
        ).check(classes);
    }
}
