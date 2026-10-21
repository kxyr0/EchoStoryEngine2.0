package io.github.kxyr0.echovisibility.forge.resource;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnosticCode;
import io.github.kxyr0.echovisibility.api.identity.VisibilityDefinitionRevision;
import io.github.kxyr0.echovisibility.api.identity.VisibilityWatchId;
import io.github.kxyr0.echovisibility.api.model.VisibilityWatchDefinition;
import io.github.kxyr0.echovisibility.core.catalog.VisibilityWatchCatalogState;
import io.github.kxyr0.echovisibility.script.EchoScriptVisibilityWatchCompiler;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VisibilityResourceLoader {
    private final VisibilityResourceDiscovery discovery = new VisibilityResourceDiscovery();
    private final EchoScriptVisibilityWatchCompiler compiler = new EchoScriptVisibilityWatchCompiler();
    private final Map<Path, VisibilityWatchId> sourceIds = new HashMap<>();
    private final Map<Path, String> sourceTexts = new HashMap<>();
    private final Map<Path, VisibilityWatchDefinition> sourceDefinitions = new HashMap<>();

    public ReloadResult reload(Path contentPackRoot, VisibilityWatchCatalogState catalog) {
        List<VisibilityDiagnostic> diagnostics = new ArrayList<>();
        Set<VisibilityWatchId> failedIds = new HashSet<>();
        int parserCalls = 0;
        int binderCalls = 0;
        try {
            List<VisibilityResourceText> discovered = discovery.discover(contentPackRoot);
            Set<Path> present = discovered.stream().map(VisibilityResourceText::path)
                    .collect(java.util.stream.Collectors.toSet());
            sourceTexts.keySet().removeIf(path -> !present.contains(path));
            sourceDefinitions.keySet().removeIf(path -> !present.contains(path));
            sourceIds.keySet().removeIf(path -> !present.contains(path));
            long nextRevision = catalog.all().stream().mapToLong(value -> value.revision().value())
                    .max().orElse(0L) + 1L;
            for (VisibilityResourceText resource : discovered) {
                if (resource.text().equals(sourceTexts.get(resource.path()))) {
                    continue;
                }
                var compiled = compiler.compile(resource.path().toString(), resource.text(),
                        new VisibilityDefinitionRevision(nextRevision++));
                parserCalls += compiled.parserCalls();
                binderCalls += compiled.binderCalls();
                diagnostics.addAll(compiled.diagnostics());
                compiled.definition().ifPresent(definition -> {
                    sourceDefinitions.put(resource.path(), definition);
                    sourceIds.put(resource.path(), definition.id());
                    sourceTexts.put(resource.path(), resource.text());
                });
                if (!compiled.successful() && sourceIds.containsKey(resource.path())) {
                    failedIds.add(sourceIds.get(resource.path()));
                }
            }
            List<VisibilityWatchDefinition> definitions = new ArrayList<>(sourceDefinitions.values());
            catalog.replaceKeepingFailures(definitions, failedIds);
        } catch (IOException | IllegalStateException exception) {
            diagnostics.add(VisibilityDiagnostic.of(VisibilityDiagnosticCode.VISIBILITY_WATCH_INVALID,
                    "Visibility reload failed: " + exception.getMessage()));
        }
        return new ReloadResult(sourceDefinitions.size(), parserCalls, binderCalls, diagnostics);
    }

    public record ReloadResult(
            int loadedDefinitions,
            int parserCalls,
            int binderCalls,
            List<VisibilityDiagnostic> diagnostics) {
        public ReloadResult {
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
