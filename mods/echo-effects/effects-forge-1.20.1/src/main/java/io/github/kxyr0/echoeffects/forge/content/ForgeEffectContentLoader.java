package io.github.kxyr0.echoeffects.forge.content;

import io.github.kxyr0.echoeffects.api.definition.EffectDefinition;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnostic;
import io.github.kxyr0.echoeffects.api.diagnostic.EffectDiagnosticCode;
import io.github.kxyr0.echoeffects.echoscript.EchoScriptEffectCompiler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ForgeEffectContentLoader {
    private final Path gameDir;
    private final EchoScriptEffectCompiler compiler = new EchoScriptEffectCompiler();

    public ForgeEffectContentLoader(Path gameDir) {
        this.gameDir = gameDir;
    }

    public ForgeEffectContentResult load() {
        List<EffectDiagnostic> diagnostics = new ArrayList<>();
        List<EffectDefinition> definitions = new ArrayList<>();
        for (Path source : sources(diagnostics)) {
            try {
                var compiled = compiler.compile(source.toString(), Files.readString(source));
                diagnostics.addAll(compiled.diagnostics());
                compiled.definition().ifPresent(definitions::add);
            } catch (IOException exception) {
                diagnostics.add(error("Cannot read effect source: " + exception.getMessage(), source));
            }
        }
        return new ForgeEffectContentResult(definitions, diagnostics);
    }

    private List<Path> sources(List<EffectDiagnostic> diagnostics) {
        Path root = gameDir.resolve("story").resolve("content-pack");
        if (!Files.isDirectory(root)) {
            return List.of();
        }
        List<Path> result = new ArrayList<>();
        try (var namespaces = Files.list(root)) {
            for (Path namespace : namespaces.filter(Files::isDirectory).sorted(Comparator.comparing(Path::toString))
                    .toList()) {
                Path effectsRoot = namespace.resolve("echo_effects").resolve("effects");
                if (!Files.isDirectory(effectsRoot)) {
                    continue;
                }
                collect(effectsRoot, result, diagnostics);
            }
        } catch (IOException exception) {
            diagnostics.add(error("Cannot list effect content packs: " + exception.getMessage(), root));
        }
        return List.copyOf(result);
    }

    private void collect(Path root, List<Path> result, List<EffectDiagnostic> diagnostics) {
        try (var stream = Files.walk(root)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".es"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(result::add);
        } catch (IOException exception) {
            diagnostics.add(error("Cannot scan effect sources: " + exception.getMessage(), root));
        }
    }

    private EffectDiagnostic error(String message, Path source) {
        return EffectDiagnostic.error(EffectDiagnosticCode.EFFECT_RESOURCE_PARSE_FAILED, message, source.toString());
    }
}
