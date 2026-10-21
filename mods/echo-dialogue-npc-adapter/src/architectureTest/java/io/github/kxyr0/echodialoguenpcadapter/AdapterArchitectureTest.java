package io.github.kxyr0.echodialoguenpcadapter;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class AdapterArchitectureTest {
    private final JavaClasses classes = new ClassFileImporter()
            .importPath(Path.of("build/classes/java/main"));

    @Test
    void adapterDoesNotUseDialogueOrNpcImplementationPackages() {
        noClasses().should().dependOnClassesThat().resideInAnyPackage(
                "io.github.kxyr0.echodialogue.core..",
                "io.github.kxyr0.echodialogue.forge.client..",
                "io.github.kxyr0.echodialogue.forge.host..",
                "io.github.kxyr0.echodialogue.forge.network..",
                "io.github.kxyr0.echodialogue.persistence..",
                "io.github.kxyr0.echodialogue.echoscript..",
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
                "io.github.kxyr0.echostoryengine..",
                "io.github.kxyr0.echostorydialogueadapter..",
                "io.github.kxyr0.echostorynpcadapter..",
                "io.github.kxyr0.echoscript..",
                "software.bernie.geckolib..",
                "net.minecraft.world.entity..",
                "net.minecraft.client..",
                "com.google.gson..",
                "java.nio.file..",
                "java.io..",
                "java.util.concurrent..")
                .check(classes);
    }

    @Test
    void adapterPackagesAreFreeOfCycles() {
        slices().matching("io.github.kxyr0.echodialoguenpcadapter.(*)..").should().beFreeOfCycles().check(classes);
    }

    @Test
    void forbiddenProductionSourceTextIsAbsent() throws IOException {
        List<String> markers = List.of(
                "java.lang.reflect",
                "ServiceLoader",
                "Class.forName",
                "ClassLoader",
                "UUID",
                ".spawn(",
                ".teleport(",
                ".despawn(",
                "providerPriority",
                "priority",
                "new Thread",
                "Executor",
                "java.nio.file",
                "java.io.File",
                "GameProfile",
                "http://",
                "https://",
                "mapping.json");
        List<String> hits;
        try (var paths = Files.walk(Path.of("src/main/java"))) {
            hits = paths.filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> sourceHits(path, markers).stream())
                    .toList();
        }
        assertTrue(hits.isEmpty(), () -> "Forbidden production source text:\n" + String.join("\n", hits));
    }

    private List<String> sourceHits(Path path, List<String> markers) {
        try {
            String text = Files.readString(path);
            return markers.stream()
                    .filter(text::contains)
                    .map(marker -> path + ": " + marker)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать production source: " + path, exception);
        }
    }
}
