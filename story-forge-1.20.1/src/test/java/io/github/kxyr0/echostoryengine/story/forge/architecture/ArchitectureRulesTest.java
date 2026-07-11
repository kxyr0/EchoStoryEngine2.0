package io.github.kxyr0.echostoryengine.story.forge.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.ExecutionStateRepository;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "io.github.kxyr0.echostoryengine.story",
        importOptions = ImportOption.DoNotIncludeTests.class)
final class ArchitectureRulesTest {
    @ArchTest
    static final ArchRule contracts_do_not_depend_on_core_forge_or_platforms = noClasses()
            .that()
            .resideInAnyPackage("..story.api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..story.core..",
                    "..story.forge..",
                    "net.minecraft..",
                    "net.minecraftforge..",
                    "com.mojang..",
                    "org.slf4j..");

    @ArchTest
    static final ArchRule core_does_not_depend_on_forge_or_minecraft = noClasses()
            .that()
            .resideInAnyPackage("..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft..", "net.minecraftforge..", "com.mojang..", "org.slf4j..");

    @ArchTest
    static final ArchRule core_layers_follow_clean_architecture = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..story.core.domain..")
            .layer("Application").definedBy("..story.core.application..")
            .layer("Ports").definedBy("..story.core.port..")
            .layer("Diagnostics").definedBy("..story.core.diagnostic..")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application")
            .whereLayer("Application").mayNotBeAccessedByAnyLayer()
            .whereLayer("Diagnostics").mayNotBeAccessedByAnyLayer()
            .whereLayer("Ports").mayOnlyBeAccessedByLayers("Application", "Diagnostics");

    @ArchTest
    static final ArchRule application_does_not_depend_on_platform_or_composition = noClasses()
            .that()
            .resideInAnyPackage("..story.core.application..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..composition..", "..infrastructure..", "..presentation..", "..story.forge..");

    @ArchTest
    static final ArchRule composition_is_not_used_by_core = noClasses()
            .that()
            .resideInAnyPackage("..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..composition..");

    @ArchTest
    static final ArchRule infrastructure_adapters_implement_ports = classes()
            .that()
            .resideInAPackage("..story.forge.infrastructure..")
            .and()
            .haveSimpleNameEndingWith("Sink")
            .should()
            .beAssignableTo(DiagnosticSink.class);

    @ArchTest
    static final ArchRule repository_adapters_implement_repository_port = classes()
            .that()
            .resideInAPackage("..story.forge.infrastructure..")
            .and()
            .haveSimpleNameEndingWith("Repository")
            .should()
            .beAssignableTo(ExecutionStateRepository.class);

    @ArchTest
    static final ArchRule clock_adapters_implement_clock_port = classes()
            .that()
            .resideInAPackage("..story.forge.infrastructure..")
            .and()
            .haveSimpleNameEndingWith("Clock")
            .should()
            .beAssignableTo(StoryClock.class);

    @ArchTest
    static final ArchRule package_slices_are_free_of_cycles = slices()
            .matching("io.github.kxyr0.echostoryengine.story.(*)..")
            .should()
            .beFreeOfCycles();

    @ArchTest
    static final ArchRule no_mutable_static_fields = fields()
            .that()
            .areStatic()
            .should()
            .beFinal();
}
