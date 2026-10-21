package io.github.kxyr0.echostoryengine.story.forge.host;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.StoryRuntimeInput;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionSaveResult;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.execution.DurableStoryRuntime;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeDispatch;
import io.github.kxyr0.echostoryengine.story.forge.execution.StoryRuntimeSavePlan;
import io.github.kxyr0.echostoryengine.story.forge.ForgeStoryTestSources;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CompiledStoryCatalogEntry;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidate;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceText;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DurableStoryRuntimeTest {
    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");
    private static final StoryId STORY_ID = StoryId.of("sample:hello_story");
    private static final ExecutionId EXECUTION_ID = ExecutionId.of("sample:test_execution");

    private final ForgeRuntimeDiagnostics diagnostics = new ForgeRuntimeDiagnostics();
    private final DurableStoryRuntime runtime = new DurableStoryRuntime(diagnostics);

    @Test
    void commandsAreAvailableOnlyAfterSuccessfulSave() {
        StoryRuntimeSavePlan plan = startPlan(entry("sample:confirm"));
        StoryRuntimeDispatch dispatch = runtime.afterSave(plan, ExecutionSaveResult.saved(List.of()), NOW);

        assertTrue(dispatch.saved());
        assertEquals(StoryExecutionPhase.WAITING, dispatch.state().phase());
        assertEquals(1, dispatch.commands().size());
    }

    @Test
    void failedSaveSuppressesExternalCommand() {
        StoryRuntimeSavePlan plan = startPlan(entry("sample:confirm"));
        StoryRuntimeDispatch dispatch = runtime.afterSave(plan, ExecutionSaveResult.failed(List.of()), NOW);

        assertFalse(dispatch.saved());
        assertEquals(StoryExecutionPhase.READY, dispatch.state().phase());
        assertTrue(dispatch.commands().isEmpty());
        assertTrue(dispatch.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-PERSISTENCE-004")));
    }

    @Test
    void incompatibleCheckpointDoesNotSaveOrDispatch() {
        CompiledStoryCatalogEntry first = entry("sample:confirm");
        StoryRuntimeSavePlan firstPlan = startPlan(first);
        CompiledStoryCatalogEntry second = entry("sample:other");

        StoryRuntimeSavePlan incompatible = runtime.afterLoad(
                StoryRuntimeInput.start(STORY_ID, EXECUTION_ID),
                second,
                ExecutionLoadResult.found(firstPlan.checkpoint().orElseThrow(), List.of()),
                NOW);

        assertFalse(incompatible.requiresSave());
        assertTrue(incompatible.diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-PERSISTENCE-003")));
    }

    private StoryRuntimeSavePlan startPlan(CompiledStoryCatalogEntry entry) {
        return runtime.afterLoad(
                StoryRuntimeInput.start(STORY_ID, EXECUTION_ID),
                entry,
                ExecutionLoadResult.notFound(List.of()),
                NOW);
    }

    private CompiledStoryCatalogEntry entry(String actionType) {
        StoryResourceCandidateCompiler compiler = new StoryResourceCandidateCompiler(
                new EchoScriptStorySourceCompiler(),
                diagnostics);
        StoryResourceCandidate candidate = compiler.compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/minimal.es",
                ForgeStoryTestSources.minimalStory(STORY_ID.asString(), actionType))), List.of(), NOW);
        return candidate.entries().get(STORY_ID);
    }
}
