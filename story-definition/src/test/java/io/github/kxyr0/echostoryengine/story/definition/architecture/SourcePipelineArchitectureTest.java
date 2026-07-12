package io.github.kxyr0.echostoryengine.story.definition.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "io.github.kxyr0.echostoryengine.story",
        importOptions = ImportOption.DoNotIncludeTests.class)
final class SourcePipelineArchitectureTest {
    @ArchTest
    static final ArchRule definition_has_no_minecraft_forge_or_mixin_imports = noClasses()
            .that()
            .resideInAnyPackage("..story.definition..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft..", "net.minecraftforge..", "org.spongepowered.asm.mixin..");

    @ArchTest
    static final ArchRule core_does_not_depend_on_definition = noClasses()
            .that()
            .resideInAnyPackage("..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.definition..");

    @ArchTest
    static final ArchRule core_does_not_depend_on_source_frontend_types = noClasses()
            .that()
            .resideInAnyPackage("..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.google.gson..", "io.github.kxyr0.echoscript..", "..story.echoscript..");

    @ArchTest
    static final ArchRule source_dtos_do_not_appear_in_runtime_state = noClasses()
            .that()
            .resideInAnyPackage("..story.api..", "..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.definition.source..");

    @ArchTest
    static final ArchRule definition_has_no_frontend_platform_or_logging_dependencies = noClasses()
            .that()
            .resideInAnyPackage("..story.definition..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "..story.forge..",
                    "..story.echoscript..",
                    "io.github.kxyr0.echoscript..",
                    "com.google.gson..",
                    "net.minecraft..",
                    "net.minecraftforge..",
                    "org.slf4j..");

    @ArchTest
    static final ArchRule core_domain_has_no_json_annotations = noClasses()
            .that()
            .resideInAnyPackage("..story.core.domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.google.gson.annotations..");

    @ArchTest
    static final ArchRule source_and_core_do_not_create_log_files = noClasses()
            .that()
            .resideInAnyPackage("..story.definition..", "..story.core..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("java.io.File..", "java.io.FileWriter..", "java.io.PrintWriter..", "java.nio.file..");

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
