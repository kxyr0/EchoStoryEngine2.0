package io.github.kxyr0.echostoryengine.story.persistence.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

@AnalyzeClasses(
        packages = "io.github.kxyr0.echostoryengine.story",
        importOptions = ImportOption.DoNotIncludeTests.class)
final class PersistenceArchitectureTest {
    @ArchTest
    static final ArchRule persistence_has_no_minecraft_forge_or_mixin_imports = noClasses()
            .that()
            .resideInAnyPackage("..story.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("net.minecraft..", "net.minecraftforge..", "org.spongepowered.asm.mixin..");

    @ArchTest
    static final ArchRule persistence_does_not_depend_on_definition_module = noClasses()
            .that()
            .resideInAnyPackage("..story.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.definition..");

    @ArchTest
    static final ArchRule definition_does_not_depend_on_persistence_module = noClasses()
            .that()
            .resideInAnyPackage("..story.definition..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule core_port_exposes_no_file_path_or_json_types = noClasses()
            .that()
            .resideInAnyPackage("..story.core.port..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("java.io..", "java.nio.file..", "com.google.gson..");

    @ArchTest
    static final ArchRule gson_is_limited_to_persistence_schema = noClasses()
            .that()
            .resideInAnyPackage("..story.persistence..")
            .and()
            .resideOutsideOfPackage("..story.persistence.schema..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.google.gson..");

    @ArchTest
    static final ArchRule save_schema_does_not_depend_on_source_dtos = noClasses()
            .that()
            .resideInAnyPackage("..story.persistence.schema..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.definition.source..");

    @ArchTest
    static final ArchRule core_and_persistence_do_not_use_java_object_serialization = noClasses()
            .that()
            .resideInAnyPackage("..story.core..", "..story.persistence..")
            .should()
            .dependOnClassesThat(equivalentTo(ObjectInputStream.class)
                    .or(equivalentTo(ObjectOutputStream.class))
                    .or(equivalentTo(Serializable.class)));

    @ArchTest
    static final ArchRule package_slices_are_free_of_cycles = slices()
            .matching("io.github.kxyr0.echostoryengine.story.(*)..")
            .should()
            .beFreeOfCycles();
}
