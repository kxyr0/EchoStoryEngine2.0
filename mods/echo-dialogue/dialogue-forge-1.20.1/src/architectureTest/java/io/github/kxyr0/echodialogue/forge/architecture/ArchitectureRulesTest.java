package io.github.kxyr0.echodialogue.forge.architecture;

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
            .importPackages("io.github.kxyr0.echodialogue");

    @Test
    void onlyForgeModuleImportsMinecraftAndForge() {
        noClasses()
                .that().resideOutsideOfPackage("..forge..")
                .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..")
                .check(classes);
    }

    @Test
    void dialogueDoesNotDependOnStoryEngineOrEchoNpc() {
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echostoryengine..",
                        "io.github.kxyr0.echonpc..")
                .check(classes);
    }

    @Test
    void coreDoesNotDependOnEchoScriptOrForge() {
        noClasses()
                .that().resideInAnyPackage("io.github.kxyr0.echodialogue.core..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echoscript..",
                        "io.github.kxyr0.echodialogue.persistence..",
                        "net.minecraft..",
                        "net.minecraftforge..")
                .check(classes);
    }

    @Test
    void persistenceDoesNotDependOnPlatformSyntaxOrClientContracts() {
        noClasses()
                .that().resideInAnyPackage("io.github.kxyr0.echodialogue.persistence..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "io.github.kxyr0.echoscript..",
                        "..forge.client..",
                        "..forge.network.message..")
                .check(classes);
    }

    @Test
    void compiledDialogueModelDoesNotContainSyntaxTreeTypes() {
        noClasses()
                .that().resideInAnyPackage("..core.model..")
                .should().dependOnClassesThat().resideInAnyPackage("io.github.kxyr0.echoscript.api.syntax..")
                .check(classes);
    }

    @Test
    void neutralVisualModelDoesNotContainMinecraftTextureOrFileTypes() {
        noClasses()
                .that().resideInAnyPackage("..api.visual..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "com.mojang.blaze3d..",
                        "java.nio.file..",
                        "java.io..",
                        "java.net..")
                .check(classes);
    }

    @Test
    void presentationApiRemainsNeutralPlainJava() {
        noClasses()
                .that().resideInAnyPackage("..presentation.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "net.minecraft..",
                        "net.minecraftforge..",
                        "io.github.kxyr0.echoscript..",
                        "io.github.kxyr0.echodialogue.persistence..",
                        "..forge.client..",
                        "..forge.network..",
                        "java.nio.file..",
                        "java.io..",
                        "java.net..",
                        "com.mojang.authlib..")
                .check(classes);
    }

    @Test
    void publicForgeBridgeDoesNotExposeDialogueHostImplementation() {
        noClasses()
                .that().resideInAnyPackage("..forge.api..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..forge.host..",
                        "..forge.presentation..",
                        "..forge.client..",
                        "..forge.network..")
                .check(classes);
    }

    @Test
    void providerRegistrationDoesNotUseDiscoveryOrReflectionMechanisms() {
        noClasses()
                .should().dependOnClassesThat().haveFullyQualifiedName("java.util.ServiceLoader")
                .check(classes);
        noClasses()
                .should().dependOnClassesThat().resideInAnyPackage("java.lang.reflect..")
                .check(classes);
    }

    @Test
    void clientVisualCodeDoesNotImportPersistenceStoryOrRemoteLookup() {
        noClasses()
                .that().resideInAnyPackage("..forge.client.visual..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "io.github.kxyr0.echodialogue.persistence..",
                        "io.github.kxyr0.echostoryengine..",
                        "com.mojang.authlib..",
                        "java.net..")
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
