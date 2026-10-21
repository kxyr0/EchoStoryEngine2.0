package io.github.kxyr0.echotrigger.forge.authoring;

import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaDiagnosticView;
import io.github.kxyr0.echotrigger.api.authoring.catalog.AreaSourceLocation;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceReloadResult;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceValidationRequest;
import io.github.kxyr0.echotrigger.api.authoring.source.TriggerSourceValidationResult;
import io.github.kxyr0.echotrigger.api.id.TriggerId;
import io.github.kxyr0.echotrigger.core.definition.CompiledTriggerDefinition;
import io.github.kxyr0.echotrigger.core.definition.TriggerDefinitionCatalog;
import io.github.kxyr0.echotrigger.forge.api.authoring.TriggerAuthoringService;
import io.github.kxyr0.echotrigger.forge.resource.TriggerContentRoots;
import io.github.kxyr0.echotrigger.forge.resource.TriggerResourceDiscovery;
import io.github.kxyr0.echotrigger.forge.resource.TriggerResourceText;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ForgeTriggerAuthoringService implements TriggerAuthoringService {
    private final ForgeTriggerServices services;
    private final TriggerContentRoots roots;
    private final TriggerResourceDiscovery discovery = new TriggerResourceDiscovery();
    private final TriggerAuthoringCompiler compiler = new TriggerAuthoringCompiler();
    private Map<TriggerId, AreaSourceLocation> activeSources = Map.of();
    private java.util.function.Supplier<net.minecraft.server.packs.resources.ResourceManager> resources = () -> null;

    public void bindResources(java.util.function.Supplier<net.minecraft.server.packs.resources.ResourceManager> value) {
        resources = java.util.Objects.requireNonNull(value);
    }

    public ForgeTriggerAuthoringService(ForgeTriggerServices services, Path gameDirectory) {
        this.services = java.util.Objects.requireNonNull(services, "services");
        this.roots = new TriggerContentRoots(gameDirectory);
    }

    @Override
    public List<TriggerAreaDefinitionView> areas() {
        return services.definitions().stream()
                .flatMap(definition -> TriggerAreaViewFactory.view(definition, source(definition.id())).stream())
                .toList();
    }

    @Override
    public long catalogRevision() {
        return services.catalogRevision();
    }

    public CanonicalReloadResult reloadCanonicalRoot() {
        long before = services.catalogRevision();
        ReloadPlan plan = loadCanonicalRoot();
        if (!plan.diagnostics().isEmpty()) {
            return new CanonicalReloadResult(false, before, before, 0, 0, plan.diagnostics());
        }
        TriggerDefinitionCatalog catalog = new TriggerDefinitionCatalog(List.copyOf(plan.definitions().values()));
        services.publishCatalog(catalog);
        activeSources = Map.copyOf(plan.sources());
        return new CanonicalReloadResult(true, before, services.catalogRevision(), catalog.definitions().size(),
                catalog.storySubscriptions().size(), List.of());
    }

    @Override
    public TriggerSourceValidationResult validate(TriggerSourceValidationRequest request) {
        TriggerAuthoringCompileResult result = compiler.compile(request, services.nextDefinitionRevision());
        Optional<TriggerAreaDefinitionView> area = result.definition().flatMap(definition ->
                TriggerAreaViewFactory.view(definition, Optional.empty()));
        List<AreaDiagnosticView> diagnostics = idDiagnostics(request, result);
        return new TriggerSourceValidationResult(result.successful() && diagnostics.isEmpty(),
                result.parserCalls(), result.binderCalls(), result.fingerprint(), area, diagnostics);
    }

    @Override
    public TriggerSourceReloadResult reload(TriggerId expectedId) {
        long before = services.catalogRevision();
        ReloadPlan plan = loadCanonicalRoot();
        if (!plan.diagnostics().isEmpty()) {
            return reloadFailure(before, expectedId, plan.diagnostics());
        }
        if (!plan.definitions().containsKey(expectedId)) {
            return missingFailure(before, expectedId);
        }
        services.publishCatalog(new TriggerDefinitionCatalog(List.copyOf(plan.definitions().values())));
        activeSources = Map.copyOf(plan.sources());
        return new TriggerSourceReloadResult(true, before, services.catalogRevision(), expectedId,
                source(expectedId), List.of());
    }

    private ReloadPlan loadCanonicalRoot() {
        Map<TriggerId, CompiledTriggerDefinition> definitions = new LinkedHashMap<>();
        Map<TriggerId, AreaSourceLocation> sources = new LinkedHashMap<>();
        java.util.ArrayList<AreaDiagnosticView> diagnostics = new java.util.ArrayList<>();
        for (TriggerResourceText resource : readResources(diagnostics)) {
            addResource(resource, definitions, sources, diagnostics);
        }
        return new ReloadPlan(definitions, sources, diagnostics);
    }

    private List<TriggerResourceText> readResources(List<AreaDiagnosticView> diagnostics) {
        try {
            var found = new java.util.ArrayList<>(discovery.externalTriggers(roots));
            found.addAll(new io.github.kxyr0.echotrigger.forge.resource.inline.InlineResourceDiscovery()
                    .discover(resources.get(), found));
            return List.copyOf(found);
        } catch (IOException exception) {
            diagnostics.add(TriggerAreaViewFactory.diagnostic("TRIGGER_RELOAD_IO_FAILED", exception.getMessage()));
            return List.of();
        }
    }

    private void addResource(
            TriggerResourceText resource,
            Map<TriggerId, CompiledTriggerDefinition> definitions,
            Map<TriggerId, AreaSourceLocation> sources,
            List<AreaDiagnosticView> diagnostics
    ) {
        TriggerSourceValidationRequest request = new TriggerSourceValidationRequest(
                new TriggerId("jdh:placeholder"), resource.path().toString(), resource.text());
        var result = compiler.compileCatalog(request, services.nextDefinitionRevision());
        diagnostics.addAll(result.diagnostics());
        result.definitions().forEach(definition -> addDefinition(resource, definition, definitions, sources,
                diagnostics));
    }

    private void addDefinition(
            TriggerResourceText resource,
            CompiledTriggerDefinition definition,
            Map<TriggerId, CompiledTriggerDefinition> definitions,
            Map<TriggerId, AreaSourceLocation> sources,
            List<AreaDiagnosticView> diagnostics
    ) {
        if (definitions.containsKey(definition.id())) {
            diagnostics.add(TriggerAreaViewFactory.diagnostic("TRIGGER_EXTERNAL_SOURCE_CONFLICT",
                    "Duplicate TriggerId in canonical trigger root: " + definition.id()));
            return;
        }
        definitions.put(definition.id(), definition);
        sources.put(definition.id(), TriggerAreaViewFactory.source(resource, roots.contentPackRoot()));
    }

    private List<AreaDiagnosticView> idDiagnostics(
            TriggerSourceValidationRequest request,
            TriggerAuthoringCompileResult result
    ) {
        java.util.ArrayList<AreaDiagnosticView> diagnostics = new java.util.ArrayList<>(result.diagnostics());
        result.definition().ifPresent(definition -> {
            if (!definition.id().equals(request.expectedId())) {
                diagnostics.add(TriggerAreaViewFactory.diagnostic("TRIGGER_ID_MISMATCH",
                        "Source declared " + definition.id() + " but expected " + request.expectedId()));
            }
        });
        return List.copyOf(diagnostics);
    }

    private TriggerSourceReloadResult reloadFailure(
            long before,
            TriggerId expectedId,
            List<AreaDiagnosticView> diagnostics
    ) {
        return new TriggerSourceReloadResult(false, before, before, expectedId, source(expectedId), diagnostics);
    }

    private TriggerSourceReloadResult missingFailure(long before, TriggerId expectedId) {
        return reloadFailure(before, expectedId, List.of(TriggerAreaViewFactory.diagnostic(
                "TRIGGER_CATALOG_VERIFY_FAILED", "Catalog does not contain expected TriggerId: " + expectedId)));
    }

    private Optional<AreaSourceLocation> source(TriggerId id) {
        return Optional.ofNullable(activeSources.get(id));
    }

    private record ReloadPlan(
            Map<TriggerId, CompiledTriggerDefinition> definitions,
            Map<TriggerId, AreaSourceLocation> sources,
            List<AreaDiagnosticView> diagnostics
    ) {
        private ReloadPlan {
            definitions = Map.copyOf(definitions);
            sources = Map.copyOf(sources);
            diagnostics = List.copyOf(diagnostics);
        }
    }

    public record CanonicalReloadResult(
            boolean successful,
            long beforeRevision,
            long afterRevision,
            int definitions,
            int sleepSubscriptions,
            List<AreaDiagnosticView> diagnostics
    ) {
        public CanonicalReloadResult {
            diagnostics = List.copyOf(diagnostics);
        }
    }
}
