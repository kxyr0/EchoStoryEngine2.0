package io.github.kxyr0.echostorynpcadapter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class AdapterArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter().importPackages("io.github.kxyr0.echostorynpcadapter");

    @Test
    void adapterDoesNotUseStoryOrNpcImplementationPackages() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "io.github.kxyr0.echostoryengine.story.core..",
                "io.github.kxyr0.echostoryengine.story.forge..",
                "io.github.kxyr0.echostoryengine.story.persistence..",
                "io.github.kxyr0.echostoryengine.story.echoscript..",
                "io.github.kxyr0.echostoryengine.story.definition..",
                "io.github.kxyr0.echonpc.core..",
                "io.github.kxyr0.echonpc.persistence..",
                "io.github.kxyr0.echonpc.forge.host..",
                "io.github.kxyr0.echonpc.forge.entity..",
                "io.github.kxyr0.echonpc.forge.client..")
                .check(classes);
    }

    @Test
    void forbiddenSystemsAreAbsent() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "io.github.kxyr0.echoscript..",
                "io.github.kxyr0.echodialogue..",
                "software.bernie.geckolib..",
                "net.minecraft.world.entity.ai..",
                "com.google.gson..",
                "java.nio.file..",
                "java.io..",
                "java.util.concurrent.Executor..")
                .check(classes);
    }

    @Test
    void adapterPackagesAreFreeOfCycles() {
        slices().matching("io.github.kxyr0.echostorynpcadapter.(*)..").should().beFreeOfCycles().check(classes);
    }
}
