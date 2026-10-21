package io.github.kxyr0.echostoryengine.story.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EchoScriptEnvironmentSourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.ForgeStoryTestSources;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.junit.jupiter.api.Test;

final class ForgeStoryResourceReloadListenerTest {
    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");

    @Test
    void listenerDiscoversUtf8EsResourcesAndInstallsCatalog() {
        StoryCatalogState catalogState = new StoryCatalogState(NOW);
        EnvironmentCatalogState environmentCatalogState = new EnvironmentCatalogState(NOW);
        InMemoryDiagnosticBuffer diagnostics = new InMemoryDiagnosticBuffer(16);
        ForgeRuntimeDiagnostics forgeDiagnostics = new ForgeRuntimeDiagnostics();
        ForgeStoryResourceReloadListener listener = new ForgeStoryResourceReloadListener(
                new StoryResourceCandidateCompiler(new EchoScriptStorySourceCompiler(), forgeDiagnostics),
                new EnvironmentResourceCandidateCompiler(new EchoScriptEnvironmentSourceCompiler()),
                catalogState,
                environmentCatalogState,
                diagnostics,
                new ForgeStoryClock(),
                forgeDiagnostics);
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                "sample",
                "echo_story/stories/from_manager.es");
        FakeReloadResourceManager resources = new FakeReloadResourceManager(Map.of(location,
                FakeReloadResourceManager.resource(
                ForgeStoryTestSources.minimalStory("sample:from_manager", "sample:action"))));

        StoryResourceReloadCandidate candidate = listener.prepare(resources, null);
        listener.apply(candidate, resources, null);

        assertTrue(candidate.stories().accepted());
        assertEquals(1, candidate.stories().sourceCount());
        assertTrue(catalogState.current().find(StoryId.of("sample:from_manager")).isPresent());
        assertEquals("ACCEPTED", catalogState.latestReload().status());
    }

    @Test
    void listenerRejectsMoreThanSevenEsResourcesInOneFolder() {
        StoryCatalogState catalogState = new StoryCatalogState(NOW);
        EnvironmentCatalogState environmentCatalogState = new EnvironmentCatalogState(NOW);
        InMemoryDiagnosticBuffer diagnostics = new InMemoryDiagnosticBuffer(16);
        ForgeRuntimeDiagnostics forgeDiagnostics = new ForgeRuntimeDiagnostics();
        ForgeStoryResourceReloadListener listener = new ForgeStoryResourceReloadListener(
                new StoryResourceCandidateCompiler(new EchoScriptStorySourceCompiler(), forgeDiagnostics),
                new EnvironmentResourceCandidateCompiler(new EchoScriptEnvironmentSourceCompiler()),
                catalogState,
                environmentCatalogState,
                diagnostics,
                new ForgeStoryClock(),
                forgeDiagnostics);
        Map<ResourceLocation, Resource> discovered = new LinkedHashMap<>();
        for (int index = 0; index < 8; index++) {
            ResourceLocation location = ResourceLocation.fromNamespaceAndPath(
                    "sample",
                    "echo_story/stories/limited/story_" + index + ".es");
            discovered.put(location, FakeReloadResourceManager.resource(ForgeStoryTestSources.minimalStory(
                    "sample:limited_" + index,
                    "sample:action")));
        }
        FakeReloadResourceManager resources = new FakeReloadResourceManager(discovered);

        StoryResourceReloadCandidate candidate = listener.prepare(resources, null);
        listener.apply(candidate, resources, null);

        assertFalse(candidate.stories().accepted());
        assertEquals(8, candidate.stories().sourceCount());
        assertTrue(candidate.stories().diagnostics().stream()
                .anyMatch(diagnostic -> diagnostic.code().equals("ESE-FORGE-RESOURCE-005")
                        && diagnostic.context().get("folder").equals("sample:echo_story/stories/limited")
                        && diagnostic.context().get("scripts").equals("8")
                        && diagnostic.context().get("limit").equals("7")));
        assertEquals("REJECTED", catalogState.latestReload().status());
        assertEquals(0, catalogState.current().storyCount());
    }

}
