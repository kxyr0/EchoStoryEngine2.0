package io.github.kxyr0.echostoryengine.story.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.definition.compile.StoryCompilationResult;
import io.github.kxyr0.echostoryengine.story.definition.compile.StorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.definition.source.StorySourceInput;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.ForgeStoryTestSources;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogInstallResult;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class StoryResourceCandidateCompilerTest {
    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");

    private final StoryResourceCandidateCompiler compiler = new StoryResourceCandidateCompiler(
            new EchoScriptStorySourceCompiler(),
            new ForgeRuntimeDiagnostics());

    @Test
    void validStoryResourceBuildsCatalogCandidate() {
        StoryResourceCandidate candidate = compiler.compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/minimal.es",
                ForgeStoryTestSources.minimalStory("sample:hello_story", "sample:confirm"))), List.of(), NOW);

        assertTrue(candidate.accepted());
        assertEquals(1, candidate.entries().size());
        assertTrue(candidate.entries().containsKey(StoryId.of("sample:hello_story")));
        assertTrue(candidate.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-RESOURCE-001")));
    }

    @Test
    void duplicateStoryWithDifferentFingerprintRejectsCandidate() {
        StoryResourceCandidate candidate = compiler.compile(List.of(
                new StoryResourceText(
                        "sample:echo_story/stories/first.es",
                        ForgeStoryTestSources.minimalStory("sample:hello_story", "sample:confirm")),
                new StoryResourceText(
                        "sample:echo_story/stories/second.es",
                        ForgeStoryTestSources.minimalStory("sample:hello_story", "sample:other"))), List.of(), NOW);

        assertFalse(candidate.accepted());
        assertTrue(candidate.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-RESOURCE-003")));
    }

    @Test
    void invalidCandidateRetainsPreviouslyInstalledCatalog() {
        StoryCatalogState state = new StoryCatalogState(NOW);
        StoryResourceCandidate valid = compiler.compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/minimal.es",
                ForgeStoryTestSources.minimalStory("sample:hello_story", "sample:confirm"))), List.of(), NOW);
        state.install(valid, NOW);

        StoryResourceCandidate invalid = compiler.compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/broken.es",
                "class Broken : StoryResource {")), List.of(), NOW);
        CatalogInstallResult result = state.install(invalid, NOW.plusSeconds(1));

        assertFalse(result.installed());
        assertEquals(1, result.current().storyCount());
        assertEquals(1, state.current().storyCount());
    }

    @Test
    void unchangedSourceTextIsNotCompiledAgain() {
        CountingStorySourceCompiler sourceCompiler = new CountingStorySourceCompiler();
        StoryResourceCandidateCompiler cachedCompiler = new StoryResourceCandidateCompiler(
                sourceCompiler,
                new ForgeRuntimeDiagnostics());
        StoryResourceText source = new StoryResourceText("sample:echo_story/stories/live.es", "story one");

        cachedCompiler.compile(List.of(source), List.of(), NOW);
        cachedCompiler.compile(List.of(source), List.of(), NOW.plusSeconds(1));
        cachedCompiler.compile(List.of(new StoryResourceText(source.sourceId(), "story two")), List.of(),
                NOW.plusSeconds(2));

        assertEquals(2, sourceCompiler.compileCount());
    }

    @Test
    void deletedSourceDropsCachedCompile() {
        CountingStorySourceCompiler sourceCompiler = new CountingStorySourceCompiler();
        StoryResourceCandidateCompiler cachedCompiler = new StoryResourceCandidateCompiler(
                sourceCompiler,
                new ForgeRuntimeDiagnostics());
        StoryResourceText source = new StoryResourceText("sample:echo_story/stories/live.es", "story one");

        cachedCompiler.compile(List.of(source), List.of(), NOW);
        cachedCompiler.compile(List.of(), List.of(), NOW.plusSeconds(1));
        cachedCompiler.compile(List.of(source), List.of(), NOW.plusSeconds(2));

        assertEquals(2, sourceCompiler.compileCount());
    }

    private static final class CountingStorySourceCompiler implements StorySourceCompiler {
        private int compileCount;

        @Override
        public StoryCompilationResult compile(StorySourceInput input, Instant occurredAt) {
            compileCount++;
            return new StoryCompilationResult(
                    true,
                    true,
                    true,
                    true,
                    input.sourceId(),
                    Optional.empty(),
                    Optional.empty(),
                    List.of());
        }

        private int compileCount() {
            return compileCount;
        }
    }
}
