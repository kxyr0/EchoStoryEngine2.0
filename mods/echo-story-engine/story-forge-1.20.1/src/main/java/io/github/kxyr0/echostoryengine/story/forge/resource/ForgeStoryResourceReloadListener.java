package io.github.kxyr0.echostoryengine.story.forge.resource;

import io.github.kxyr0.echostoryengine.story.core.port.DiagnosticSink;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogInstallResult;
import io.github.kxyr0.echostoryengine.story.forge.environment.catalog.EnvironmentCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidate;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.environment.resource.EnvironmentSourceText;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.CatalogInstallResult;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.resource.discovery.StoryResourceDiscovery;
import io.github.kxyr0.echostoryengine.story.forge.resource.discovery.StoryResourceReadResult;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Forge reload listener for server data resources under `echo_story/stories`.
 */
public final class ForgeStoryResourceReloadListener
        extends SimplePreparableReloadListener<StoryResourceReloadCandidate> {
    private final StoryResourceCandidateCompiler compiler;
    private final EnvironmentResourceCandidateCompiler environmentCompiler;
    private final StoryCatalogState catalogState;
    private final EnvironmentCatalogState environmentCatalogState;
    private final DiagnosticSink diagnosticSink;
    private final StoryClock clock;
    private final StoryResourceDiscovery discovery;
    private final StoryReloadChatNotifier chatNotifier;
    private final Supplier<Optional<MinecraftServer>> server;

    public ForgeStoryResourceReloadListener(
            StoryResourceCandidateCompiler compiler,
            EnvironmentResourceCandidateCompiler environmentCompiler,
            StoryCatalogState catalogState,
            EnvironmentCatalogState environmentCatalogState,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        this(compiler, environmentCompiler, catalogState, environmentCatalogState, diagnosticSink, clock,
                diagnostics, Optional::empty);
    }

    public ForgeStoryResourceReloadListener(
            StoryResourceCandidateCompiler compiler,
            EnvironmentResourceCandidateCompiler environmentCompiler,
            StoryCatalogState catalogState,
            EnvironmentCatalogState environmentCatalogState,
            DiagnosticSink diagnosticSink,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics,
            Supplier<Optional<MinecraftServer>> server) {
        this.compiler = Objects.requireNonNull(compiler, "compiler");
        this.environmentCompiler = Objects.requireNonNull(environmentCompiler, "environmentCompiler");
        this.catalogState = Objects.requireNonNull(catalogState, "catalogState");
        this.environmentCatalogState = Objects.requireNonNull(environmentCatalogState, "environmentCatalogState");
        this.diagnosticSink = Objects.requireNonNull(diagnosticSink, "diagnosticSink");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.discovery = new StoryResourceDiscovery(clock, Objects.requireNonNull(diagnostics, "diagnostics"));
        this.chatNotifier = new StoryReloadChatNotifier();
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public String getName() {
        return "echo_story_engine_story_resources";
    }

    @Override
    protected StoryResourceReloadCandidate prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        StoryResourceReadResult storyRead = discovery.discoverStories(resourceManager);
        StoryResourceReadResult environmentRead = discovery.discoverEnvironments(resourceManager);
        return new StoryResourceReloadCandidate(
                compiler.compile(storyRead.resources(), storyRead.diagnostics(), clock.now()),
                environmentCompiler.compile(environmentSources(environmentRead), environmentRead.diagnostics(),
                        clock.now()));
    }

    @Override
    protected void apply(
            StoryResourceReloadCandidate candidate,
            ResourceManager resourceManager,
            ProfilerFiller profiler) {
        install(candidate);
    }

    private void install(StoryResourceReloadCandidate candidate) {
        CatalogInstallResult stories = catalogState.install(candidate.stories(), clock.now());
        EnvironmentCatalogInstallResult environments =
                environmentCatalogState.install(candidate.environments(), clock.now());
        stories.diagnostics().forEach(diagnosticSink::accept);
        environments.diagnostics().forEach(diagnosticSink::accept);
        chatNotifier.notifyErrors(server.get(), stories.diagnostics());
        chatNotifier.notifyErrors(server.get(), environments.diagnostics());
    }

    private java.util.List<EnvironmentSourceText> environmentSources(StoryResourceReadResult read) {
        return read.resources().stream()
                .map(resource -> new EnvironmentSourceText(resource.sourceId(), resource.content()))
                .toList();
    }
}
