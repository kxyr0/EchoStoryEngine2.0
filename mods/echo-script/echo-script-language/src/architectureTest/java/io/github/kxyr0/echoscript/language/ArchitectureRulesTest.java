package io.github.kxyr0.echoscript.language;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRulesTest {
    private static final List<String> FORBIDDEN_TEXT = List.of(
            "net.minecraft",
            "net.minecraftforge",
            "net.fabricmc",
            "javax.script",
            "JavaCompiler",
            "Class.forName",
            "MethodHandle",
            "getDeclaredMethod",
            ".invoke(",
            "Logger",
            "logFile",
            "StoryResource"
    );

    @Test
    void modulesKeepTheRequiredGradleDependencyDirection() throws IOException {
        Path root = moduleRoot().getParent();
        String apiBuild = Files.readString(root.resolve("echo-script-api/build.gradle"), StandardCharsets.UTF_8);
        String languageBuild = Files.readString(
                root.resolve("echo-script-language/build.gradle"),
                StandardCharsets.UTF_8
        );

        assertFalse(apiBuild.contains("project("));
        assertTrue(languageBuild.contains("api project(':echo-script-api')"));
        assertFalse(languageBuild.contains("net.minecraft"));
        assertFalse(languageBuild.contains("net.minecraftforge"));
        assertFalse(languageBuild.contains("net.fabricmc"));
    }

    @Test
    void productionSourcesDoNotUseForbiddenTechnologiesOrDomainImports() throws IOException {
        for (Path source : productionSources()) {
            String text = Files.readString(source, StandardCharsets.UTF_8);
            for (String forbidden : FORBIDDEN_TEXT) {
                assertFalse(text.contains(forbidden), source + " contains forbidden text " + forbidden);
            }
            assertFalse(Pattern.compile("\\bstatic\\s+(?!final\\b)[^;{=]+[;=]").matcher(text).find());
            assertFalse(source.toString()
                    .matches("(?i).*[\\\\/](common|utils?|helpers?|manager|processor|handler)[\\\\/].*"));
        }
    }

    @Test
    void packageSlicesHaveNoCycles() {
        JavaClasses classes = new ClassFileImporter().importPackages("io.github.kxyr0.echoscript");

        slices().matching("io.github.kxyr0.echoscript.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void productionClassAndMethodHardLimitsAreRespected() throws IOException {
        for (Path source : productionSources()) {
            List<String> lines = Files.readAllLines(source, StandardCharsets.UTF_8);
            long logicalLines = lines.stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && !line.startsWith("*") && !line.startsWith("//"))
                    .count();
            assertTrue(logicalLines <= 180, source + " exceeds production class limit");
        }
    }

    private static List<Path> productionSources() throws IOException {
        Path root = moduleRoot().getParent();
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> path.toString().contains("\\src\\main\\java\\"))
                    .toList();
        }
    }

    private static Path moduleRoot() {
        return Path.of(System.getProperty("user.dir"));
    }
}
