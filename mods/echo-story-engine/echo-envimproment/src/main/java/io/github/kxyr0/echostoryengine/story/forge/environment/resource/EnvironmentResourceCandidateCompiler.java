package io.github.kxyr0.echostoryengine.story.forge.environment.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentSourceCompiler;
import io.github.kxyr0.echostoryengine.story.definition.compile.EnvironmentSourceInput;
import io.github.kxyr0.echostoryengine.story.definition.environment.model.EnvironmentResourceDef;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogEntry;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compiles EnvironmentResource texts into an immutable catalog candidate.
 */
public final class EnvironmentResourceCandidateCompiler {
    private final EnvironmentSourceCompiler compiler;
    private final ForgeEnvironmentResourceValidator forgeValidator = new ForgeEnvironmentResourceValidator();
    private final ForgeEnvironmentResourceDiagnostics diagnostics = new ForgeEnvironmentResourceDiagnostics();
    private final Map<String, CachedEnvironmentCompile> cache = new HashMap<>();

    public EnvironmentResourceCandidateCompiler(EnvironmentSourceCompiler compiler) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
    }

    public synchronized EnvironmentResourceCandidate compile(
            Collection<EnvironmentSourceText> resources,
            List<StoryDiagnostic> readDiagnostics,
            Instant occurredAt) {
        CandidateBuild build = new CandidateBuild(new LinkedHashMap<>(), new ArrayList<>(readDiagnostics), occurredAt);
        cache.keySet().retainAll(sourceIds(resources));
        for (EnvironmentSourceText resource : ordered(resources)) {
            compileOne(resource, build);
        }
        int errorCount = countErrors(build.diagnostics());
        addSummary(build, resources.size(), errorCount);
        return new EnvironmentResourceCandidate(
                errorCount == 0, build.entries(), build.diagnostics(), resources.size(), errorCount);
    }

    private void compileOne(EnvironmentSourceText resource, CandidateBuild build) {
        CachedEnvironmentCompile result = cached(resource, build.occurredAt());
        build.diagnostics().addAll(result.diagnostics());
        result.resource().ifPresent(environment -> putEnvironment(resource, environment, build));
    }

    private CachedEnvironmentCompile cached(EnvironmentSourceText resource, Instant occurredAt) {
        CachedEnvironmentCompile current = cache.get(resource.sourceId());
        if (current != null && current.matches(resource.content())) {
            return current;
        }
        EnvironmentCompilationResult result = compiler.compile(
                new EnvironmentSourceInput(resource.sourceId(), resource.content()), occurredAt);
        List<StoryDiagnostic> diagnostics = new ArrayList<>(result.diagnostics());
        result.environment().ifPresent(environment -> diagnostics.addAll(forgeValidator.validate(environment,
                occurredAt)));
        CachedEnvironmentCompile next = new CachedEnvironmentCompile(
                resource.content(), result.environment(), diagnostics);
        cache.put(resource.sourceId(), next);
        return next;
    }

    private void putEnvironment(
            EnvironmentSourceText resource,
            EnvironmentResourceDef environment,
            CandidateBuild build) {
        EnvironmentCatalogEntry next = new EnvironmentCatalogEntry(environment, resource.sourceId());
        EnvironmentCatalogEntry previous = build.entries().putIfAbsent(environment.id(), next);
        if (previous != null) {
            build.diagnostics().add(diagnostics.duplicateEnvironment(
                    new ForgeEnvironmentResourceDiagnostics.DuplicateEnvironment(
                            environment.id().asString(), previous.sourceId(), resource.sourceId(),
                            environment.location()),
                    build.occurredAt()));
        }
    }

    private void addSummary(CandidateBuild build, int sourceCount, int errorCount) {
        if (errorCount == 0) {
            build.diagnostics().add(diagnostics.catalogInstalled(build.entries().size(), sourceCount,
                    build.occurredAt()));
        } else {
            build.diagnostics().add(diagnostics.catalogRejected(sourceCount, errorCount, build.occurredAt()));
        }
    }

    private List<EnvironmentSourceText> ordered(Collection<EnvironmentSourceText> resources) {
        return resources.stream()
                .sorted((left, right) -> left.sourceId().compareTo(right.sourceId()))
                .toList();
    }

    private Set<String> sourceIds(Collection<EnvironmentSourceText> resources) {
        return resources.stream().map(EnvironmentSourceText::sourceId).collect(Collectors.toSet());
    }

    private int countErrors(List<StoryDiagnostic> diagnostics) {
        return (int) diagnostics.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
                .count();
    }

    private record CandidateBuild(
            Map<NamespacedId, EnvironmentCatalogEntry> entries,
            List<StoryDiagnostic> diagnostics,
            Instant occurredAt) {
    }

    private record CachedEnvironmentCompile(
            String sourceText,
            Optional<EnvironmentResourceDef> resource,
            List<StoryDiagnostic> diagnostics) {
        private CachedEnvironmentCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            resource = Objects.requireNonNull(resource, "resource");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }
}
