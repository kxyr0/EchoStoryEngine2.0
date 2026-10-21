package io.github.kxyr0.echostoryengine.story.echoscript;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(
        packages = "io.github.kxyr0.echostoryengine.story",
        importOptions = ImportOption.DoNotIncludeTests.class)
final class EchoScriptAdapterArchitectureTest {
    @ArchTest
    static final ArchRule adapter_has_no_platform_or_logging_dependencies = noClasses()
            .that()
            .resideInAnyPackage("..story.echoscript..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "net.minecraft..",
                    "net.minecraftforge..",
                    "org.spongepowered.asm.mixin..",
                    "org.slf4j..",
                    "org.apache.logging.log4j..",
                    "java.util.logging..");

    @ArchTest
    static final ArchRule lower_modules_do_not_depend_on_adapter_or_echoscript = noClasses()
            .that()
            .resideInAnyPackage("..story.api..", "..story.core..", "..story.definition..", "..story.persistence..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..story.echoscript..", "io.github.kxyr0.echoscript..");

    @ArchTest
    static final ArchRule compiled_runtime_and_persistence_do_not_expose_echoscript = noClasses()
            .that()
            .resideInAnyPackage("..story.api..", "..story.core.domain..", "..story.persistence.schema..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("io.github.kxyr0.echoscript..");

    @Test
    void gradleUsesPublishedDependencyWithoutJarsOrAbsolutePaths() throws IOException {
        Path root = repositoryRoot();
        String settings = read(root.resolve("settings.gradle"));
        String properties = read(root.resolve("gradle.properties"));
        String adapterBuild = read(root.resolve("story-echoscript-adapter/build.gradle"));
        String definitionBuild = read(root.resolve("story-definition/build.gradle"));

        assertTrue(settings.contains("story-echoscript-adapter"));
        assertTrue(properties.contains("echo_script_version=0.5.0-SNAPSHOT"));
        assertTrue(adapterBuild.contains("io.github.kxyr0.echoscript:echo-script-language:${echo_script_version}"));
        assertTrue(adapterBuild.contains("implementation"));
        assertFalse(adapterBuild.contains("api \"io.github.kxyr0.echoscript"));
        assertFalse(adapterBuild.contains("files("));
        assertFalse(adapterBuild.contains("D:\\"));
        assertFalse(definitionBuild.contains("gson"));
        assertTrue(jarFiles(root).isEmpty());
    }

    @Test
    void productionCodeContainsNoCopiedEchoscriptSourceOrDialogueSemantics() throws IOException {
        Path root = repositoryRoot();
        List<Path> sources = productionSources(root);
        for (Path source : sources) {
            String text = read(source);
            if (!source.toString().contains("story-echoscript-adapter")
                    && !source.toString().contains("echo-envimproment")) {
                assertFalse(text.contains("io.github.kxyr0.echoscript"));
            }
            assertFalse(text.contains("DialogueResource"));
            assertFalse(text.contains("Line("));
            assertFalse(text.contains("Alias("));
        }
        assertTrue(sources.stream().noneMatch(path -> path.toString().contains("JsonStorySourceParser")));
    }

    private static Path repositoryRoot() {
        return Path.of(System.getProperty("user.dir")).getParent();
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static List<Path> jarFiles(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".jar"))
                    .filter(path -> !path.toString().contains("\\build\\"))
                    .filter(path -> !path.toString().contains("\\gradle\\wrapper\\"))
                    .toList();
        }
    }

    private static List<Path> productionSources(Path root) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("\\src\\main\\java\\"))
                    .toList();
        }
    }
}
