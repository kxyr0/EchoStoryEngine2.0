package io.github.kxyr0.echomagic.forge.content;

import io.github.kxyr0.echomagic.api.definition.SpellDefinition;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnostic;
import io.github.kxyr0.echomagic.api.diagnostic.MagicDiagnosticCode;
import io.github.kxyr0.echomagic.echoscript.EchoScriptSpellCompiler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ForgeSpellContentLoader {
    private final Path gameDir;
    private final EchoScriptSpellCompiler compiler;
    private volatile ResourceManager currentResources;

    public ForgeSpellContentLoader(Path gameDir) { this(gameDir, new EchoScriptSpellCompiler()); }
    ForgeSpellContentLoader(Path gameDir, EchoScriptSpellCompiler compiler) {
        this.gameDir = gameDir;
        this.compiler = compiler;
    }

    public ForgeSpellContentResult load(ResourceManager resources) {
        currentResources = resources;
        List<SpellDefinition> definitions = new ArrayList<>();
        List<MagicDiagnostic> diagnostics = new ArrayList<>();
        Map<ResourceLocation, Resource> found = resources.listResources("echo_magic/spells",
                id -> id.getPath().endsWith(".es"));
        found.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try (var input = entry.getValue().open()) {
                compile(entry.getKey().toString(), new String(input.readAllBytes(), StandardCharsets.UTF_8),
                        definitions, diagnostics);
            } catch (IOException exception) {
                diagnostics.add(error("Cannot read spell resource: " + exception.getMessage(), entry.getKey().toString()));
            }
        });
        loadExternal(definitions, diagnostics);
        return new ForgeSpellContentResult(definitions, diagnostics);
    }

    public ForgeSpellContentResult reloadCurrent() {
        ResourceManager resources = currentResources;
        return resources == null ? loadExternal() : load(resources);
    }

    public ForgeSpellContentResult loadExternal() {
        List<SpellDefinition> definitions = new ArrayList<>();
        List<MagicDiagnostic> diagnostics = new ArrayList<>();
        loadExternal(definitions, diagnostics);
        return new ForgeSpellContentResult(definitions, diagnostics);
    }

    private void loadExternal(List<SpellDefinition> definitions, List<MagicDiagnostic> diagnostics) {
        Path root = gameDir.resolve("story").resolve("content-pack");
        if (!Files.isDirectory(root)) return;
        try (var paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile).filter(path -> path.toString().replace('\\', '/').contains("/echo_magic/spells/"))
                    .filter(path -> path.getFileName().toString().endsWith(".es"))
                    .sorted(Comparator.comparing(Path::toString)).forEach(path -> read(path, definitions, diagnostics));
        } catch (IOException exception) {
            diagnostics.add(error("Cannot scan spell content packs: " + exception.getMessage(), root.toString()));
        }
    }

    private void read(Path path, List<SpellDefinition> definitions, List<MagicDiagnostic> diagnostics) {
        try { compile(path.toString(), Files.readString(path), definitions, diagnostics); }
        catch (IOException exception) { diagnostics.add(error("Cannot read spell source: " + exception.getMessage(), path.toString())); }
    }

    private void compile(String source, String text, List<SpellDefinition> definitions,
                         List<MagicDiagnostic> diagnostics) {
        var result = compiler.compile(source, text);
        diagnostics.addAll(result.diagnostics());
        result.definition().filter(definition -> !definitions.contains(definition))
                .ifPresent(definitions::add);
    }

    private MagicDiagnostic error(String message, String source) {
        return MagicDiagnostic.error(MagicDiagnosticCode.MAGIC_DEFINITION_BIND_FAILED, message, source);
    }
}
