package io.github.kxyr0.echotrigger.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class CoreArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("io.github.kxyr0.echotrigger.core");

    @Test
    void coreDoesNotDependOnMinecraftForgeOrDialogue() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "net.minecraft..",
                "net.minecraftforge..",
                "io.github.kxyr0.echoitem..",
                "io.github.kxyr0.echoitempack..",
                "io.github.kxyr0.echodialogue..")
                .check(classes);
    }
}
