package io.github.kxyr0.echostoryengine.story.forge.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echostoryengine.story.api.identity.NamespacedId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.echoscript.environment.EchoScriptEnvironmentSourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import java.time.Instant;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class ForgeEnvironmentResourceReloadListenerTest {
    private static final Instant NOW = Instant.parse("2026-07-12T06:00:00Z");

    @Test
    void listenerDiscoversEnvironmentResourcesAndInstallsCatalog() {
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
                "echo_story/environments/home/table.es");
        FakeReloadResourceManager resources = new FakeReloadResourceManager(Map.of(
                location, FakeReloadResourceManager.resource(environmentSource())));

        StoryResourceReloadCandidate candidate = listener.prepare(resources, null);
        listener.apply(candidate, resources, null);

        assertTrue(candidate.environments().accepted());
        assertEquals(1, candidate.environments().sourceCount());
        assertTrue(environmentCatalogState.current().find(NamespacedId.of("sample:table")).isPresent());
        assertEquals("ACCEPTED", environmentCatalogState.latestReload().status());
    }

    private static String environmentSource() {
        return """
                class TableEnvironment : EnvironmentResource
                {
                    private DimensionId World = "minecraft:overworld";
                    private Position Plate = Position(10, 64, -5);
                    private EnvironmentStateDef[] states = [Night(), MorningBreakfast()];

                    public override EnvironmentResourceDef Build()
                    {
                        return Environment("sample:table", World, states);
                    }

                    private EnvironmentStateDef Night()
                    {
                        var changes = [ClearBlock(Plate)];
                        return State("night", changes);
                    }

                    private EnvironmentStateDef MorningBreakfast()
                    {
                        var changes = [ClearBlock(Plate)];
                        return State("morning_breakfast", changes);
                    }
                }
                """;
    }
}
