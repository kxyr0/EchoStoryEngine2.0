package io.github.kxyr0.echowardrobe.core;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "io.github.kxyr0.echowardrobe")
final class WardrobeArchitectureTest {
    @ArchTest
    static final ArchRule contractsAndCoreAvoidMinecraftAndForge = noClasses()
            .that().resideInAnyPackage("..api..", "..core..")
            .should().dependOnClassesThat().resideInAnyPackage("net.minecraft..", "net.minecraftforge..");

    @ArchTest
    static final ArchRule wardrobeAvoidsForbiddenEchoImplementations = noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("..echoitem..", "..echodialogue..",
                    "..echoanimation..", "..echostoryengine..", "..echonpc..", "..echotrigger..");
}
