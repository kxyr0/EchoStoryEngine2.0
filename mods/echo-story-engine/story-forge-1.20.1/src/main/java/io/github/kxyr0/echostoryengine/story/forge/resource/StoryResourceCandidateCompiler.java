package io.github.kxyr0.echostoryengine.story.forge.resource;

import io.github.kxyr0.echostoryengine.story.api.diagnostic.DiagnosticSeverity;
import io.github.kxyr0.echostoryengine.story.api.diagnostic.StoryDiagnostic;
import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinition;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryDefinitionFingerprint;
import io.github.kxyr0.echostoryengine.story.core.domain.StoryNodeKind;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.StorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
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
 * Компилирует `.es` StoryResource texts в atomic catalog candidate.
 */
public final class StoryResourceCandidateCompiler {
    private final StorySourceCompiler compiler;
    private final ForgeRuntimeDiagnostics diagnostics;
    private final Map<String, CachedStoryCompile> cache = new HashMap<>();

    public StoryResourceCandidateCompiler(
            StorySourceCompiler compiler,
            ForgeRuntimeDiagnostics diagnostics) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public synchronized StoryResourceCandidate compile(
            Collection<StoryResourceText> resources,
            List<StoryDiagnostic> readDiagnostics,
            Instant occurredAt) {
        List<StoryDiagnostic> collected = new ArrayList<>(readDiagnostics);
        Map<StoryId, CompiledStoryCatalogEntry> entries = new LinkedHashMap<>();
        cache.keySet().retainAll(sourceIds(resources));
        CandidateBuild build = new CandidateBuild(entries, collected, occurredAt);
        for (StoryResourceText resource : ordered(resources)) {
            compileOne(resource, build);
        }
        int errorCount = countErrors(collected);
        if (errorCount == 0) {
            collected.add(diagnostics.catalogInstalled(entries.size(), resources.size(), occurredAt));
        } else {
            collected.add(diagnostics.catalogRejected(resources.size(), errorCount, occurredAt));
        }
        return new StoryResourceCandidate(errorCount == 0, entries, collected, resources.size(), errorCount);
    }

    private void compileOne(
            StoryResourceText resource,
            CandidateBuild build) {
        CachedStoryCompile result = cached(resource, build.occurredAt());
        build.diagnostics().addAll(result.diagnostics());
        result.story().ifPresent(story -> putStory(resource, story, build));
    }

    private CachedStoryCompile cached(StoryResourceText resource, Instant occurredAt) {
        CachedStoryCompile current = cache.get(resource.sourceId());
        if (current != null && current.matches(resource.content())) {
            return current;
        }
        StoryCompilationResult result = compiler.compile(
                new StorySourceInput(resource.sourceId(), resource.content()),
                occurredAt);
        CachedStoryCompile next = new CachedStoryCompile(
                resource.content(),
                result.compiledStory(),
                result.diagnostics());
        cache.put(resource.sourceId(), next);
        return next;
    }

    private void putStory(
            StoryResourceText resource,
            StoryDefinition story,
            CandidateBuild build) {
        StoryId storyId = story.storyId();
        CompiledStoryCatalogEntry next = new CompiledStoryCatalogEntry(
                story,
                StoryDefinitionFingerprint.from(story),
                resource.sourceId(),
                actionTypes(story));
        CompiledStoryCatalogEntry previous = build.entries().putIfAbsent(storyId, next);
        if (previous != null && !previous.fingerprint().equals(next.fingerprint())) {
            build.diagnostics().add(diagnostics.duplicateStory(storyId, previous.sourceId(), resource.sourceId(),
                    build.occurredAt()));
        }
    }

    private Set<NamespacedId> actionTypes(StoryDefinition story) {
        return story.nodes().values().stream()
                .filter(node -> node.kind() == StoryNodeKind.EXTERNAL_ACTION)
                .map(node -> node.actionType())
                .collect(Collectors.toUnmodifiableSet());
    }

    private List<StoryResourceText> ordered(Collection<StoryResourceText> resources) {
        return resources.stream()
                .sorted((left, right) -> left.sourceId().compareTo(right.sourceId()))
                .toList();
    }

    private Set<String> sourceIds(Collection<StoryResourceText> resources) {
        return resources.stream()
                .map(StoryResourceText::sourceId)
                .collect(Collectors.toSet());
    }

    private int countErrors(List<StoryDiagnostic> collected) {
        return (int) collected.stream()
                .filter(diagnostic -> diagnostic.severity() == DiagnosticSeverity.ERROR)
                .count();
    }

    private record CandidateBuild(
            Map<StoryId, CompiledStoryCatalogEntry> entries,
            List<StoryDiagnostic> diagnostics,
            Instant occurredAt) {
    }

    private record CachedStoryCompile(
            String sourceText,
            Optional<StoryDefinition> story,
            List<StoryDiagnostic> diagnostics) {
        private CachedStoryCompile {
            sourceText = Objects.requireNonNull(sourceText, "sourceText");
            story = Objects.requireNonNull(story, "story");
            diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
        }

        private boolean matches(String nextSourceText) {
            return sourceText.equals(nextSourceText);
        }
    }
}
