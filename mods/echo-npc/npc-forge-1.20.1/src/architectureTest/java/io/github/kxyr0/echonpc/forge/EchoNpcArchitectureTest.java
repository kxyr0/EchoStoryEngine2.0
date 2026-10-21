package io.github.kxyr0.echonpc.forge;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class EchoNpcArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("io.github.kxyr0.echonpc");

    @Test
    void neutralContractsAndCoreDoNotDependOnForgeMinecraftOrEchoScriptSyntax() {
        noClasses().that().resideInAnyPackage("io.github.kxyr0.echonpc.api..", "..core..")
                .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..")
                .check(classes);
        noClasses().that().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("io.github.kxyr0.echoscript..")
                .check(classes);
    }

    @Test
    void adapterDoesNotDependOnForgeOrMinecraft() {
        noClasses().that().resideInAPackage("..echoscript..")
                .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..")
                .check(classes);
    }

    @Test
    void coreDoesNotDependOnPersistenceImplementation() {
        noClasses().that().resideInAPackage("..core..")
                .should().dependOnClassesThat().resideInAPackage("io.github.kxyr0.echonpc.persistence..")
                .check(classes);
    }

    @Test
    void persistenceDoesNotDependOnForgeMinecraftOrEchoScript() {
        noClasses().that().resideInAPackage("io.github.kxyr0.echonpc.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..",
                        "io.github.kxyr0.echoscript..")
                .check(classes);
    }

    @Test
    void publicContractsAndCorePortDoNotExposeFileOrJsonTypes() {
        noClasses().that().resideInAnyPackage("io.github.kxyr0.echonpc.api..", "..core.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage("java.nio.file..", "java.io..",
                        "com.google.gson..")
                .check(classes);
    }

    @Test
    void forgeApiBridgeDoesNotDependOnRuntimeImplementation() {
        noClasses().that().resideInAPackage("..forge.api..")
                .should().dependOnClassesThat().resideInAnyPackage("..forge.host..", "..forge.entity..",
                        "..forge.command..", "..forge.resource..", "..forge.client..")
                .check(classes);
    }

    @Test
    void clientRendererDoesNotDependOnEchoScriptOrServerResourceLoading() {
        noClasses().that().resideInAPackage("..forge.client..")
                .should().dependOnClassesThat().resideInAnyPackage("..echoscript..", "..forge.resource..")
                .check(classes);
    }

    @Test
    void forbiddenExternalSystemsAreAbsent() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "io.github.kxyr0.echostoryengine..",
                "io.github.kxyr0.echodialogue..",
                "software.bernie.geckolib..",
                "net.minecraft.world.entity.ai.navigation..")
                .check(classes);
    }

    @Test
    void publicApiDoesNotDependOnStoryTypes() {
        noClasses().that().resideInAPackage("io.github.kxyr0.echonpc.api..")
                .should().dependOnClassesThat().resideInAPackage("io.github.kxyr0.echostoryengine..")
                .check(classes);
    }

    @Test
    void echoNpcPackagesAreFreeOfCycles() {
        slices().matching("io.github.kxyr0.echonpc.(*)..").should().beFreeOfCycles().check(classes);
    }
}
