package io.github.kxyr0.echoplayersleep.core.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

final class CoreArchitectureTest {
    @Test
    void coreDoesNotImportMinecraftOrForge() {
        JavaClasses classes = new ClassFileImporter().importPackages("io.github.kxyr0.echoplayersleep.core");

        noClasses().should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..")
                .check(classes);
    }
}
