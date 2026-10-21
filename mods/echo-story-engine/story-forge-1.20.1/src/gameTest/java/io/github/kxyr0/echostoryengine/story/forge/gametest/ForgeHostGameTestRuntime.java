package io.github.kxyr0.echostoryengine.story.forge.gametest;

import io.github.kxyr0.echostoryengine.story.api.StoryExecutionPhase;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalCommand;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.core.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadResult;
import io.github.kxyr0.echostoryengine.story.core.port.persistence.ExecutionLoadStatus;
import io.github.kxyr0.echostoryengine.story.core.port.StoryClock;
import io.github.kxyr0.echostoryengine.story.echoscript.EchoScriptStorySourceCompiler;
import io.github.kxyr0.echostoryengine.story.forge.EchoStoryEngineMod;
import io.github.kxyr0.echostoryengine.story.forge.diagnostic.ForgeRuntimeDiagnostics;
import io.github.kxyr0.echostoryengine.story.forge.execution.ExternalCommandSink;
import io.github.kxyr0.echostoryengine.story.forge.host.ForgeServerHost;
import io.github.kxyr0.echostoryengine.story.forge.host.catalog.StoryCatalogState;
import io.github.kxyr0.echostoryengine.story.forge.infrastructure.ForgeStoryClock;
import io.github.kxyr0.echostoryengine.story.forge.persistence.BoundedPersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.forge.persistence.PersistenceWorkQueue;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidate;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceCandidateCompiler;
import io.github.kxyr0.echostoryengine.story.forge.resource.StoryResourceText;
import io.github.kxyr0.echostoryengine.story.persistence.file.FileExecutionStateRepository;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

final class ForgeHostGameTestRuntime {
    private ForgeHostGameTestRuntime() {
    }

    static TestContext createContext(MinecraftServer server, String storageName) {
        CatalogContext catalog = catalogContext();
        FileExecutionStateRepository repository = new FileExecutionStateRepository(
                root(server, storageName),
                catalog.clock());
        return new TestContext(server, catalog.catalogState(), repository, catalog.buffer(), catalog.clock(),
                catalog.diagnostics());
    }

    static CatalogContext catalogContext() {
        StoryClock clock = new ForgeStoryClock();
        ForgeRuntimeDiagnostics diagnostics = new ForgeRuntimeDiagnostics();
        InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(64);
        StoryCatalogState catalogState = new StoryCatalogState(clock.now());
        installCatalog(catalogState, diagnostics, clock);
        return new CatalogContext(catalogState, buffer, clock, diagnostics);
    }

    static StoryResourceCandidate invalidCandidate(CatalogContext context) {
        return compiler(context.diagnostics()).compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/broken.es",
                "class Broken : StoryResource {")), List.of(), context.clock().now());
    }

    static void assertWaitingSaved(FileExecutionStateRepository repository, ExternalCommand command) {
        ExecutionLoadResult loaded = repository.load(ForgeHostGameTests.EXECUTION_ID);
        if (loaded.status() != ExecutionLoadStatus.FOUND) {
            throw new AssertionError("WAITING checkpoint must exist before dispatch");
        }
        var checkpoint = loaded.checkpoint().orElseThrow();
        if (checkpoint.state().phase() != StoryExecutionPhase.WAITING) {
            throw new AssertionError("checkpoint must be WAITING before dispatch");
        }
        boolean sameAction = checkpoint.pendingAction()
                .map(pending -> pending.actionExecutionId().equals(command.actionExecutionId()))
                .orElse(false);
        if (!sameAction) {
            throw new AssertionError("pending action id must match dispatched command");
        }
    }

    static void waitForWaitingCheckpoint(
            GameTestHelper helper,
            FileExecutionStateRepository repository,
            ActionExecutionId actionId) {
        helper.assertTrue(actionId != null, "command action id must be captured");
        ExecutionLoadResult loaded = repository.load(ForgeHostGameTests.EXECUTION_ID);
        helper.assertTrue(loaded.status() == ExecutionLoadStatus.FOUND, "WAITING checkpoint must exist");
        var checkpoint = loaded.checkpoint().orElseThrow();
        helper.assertTrue(checkpoint.state().phase() == StoryExecutionPhase.WAITING,
                "checkpoint must stay WAITING before host recreation");
        helper.assertTrue(checkpoint.pendingAction()
                .map(pending -> pending.actionExecutionId().equals(actionId))
                .orElse(false), "same ActionExecutionId must be recoverable");
    }

    static ExternalCommandSink unexpectedCommandSink() {
        return (state, command) -> {
            throw new AssertionError("recovered completion must not dispatch a new command");
        };
    }

    static ExternalCommandSink recoveryOnlySink(ActionExecutionId expectedActionId) {
        return new ExternalCommandSink() {
            @Override
            public void dispatch(
                    io.github.kxyr0.echostoryengine.story.api.StoryExecutionState state,
                    ExternalCommand command) {
                throw new AssertionError("recovery must not dispatch a fresh command");
            }

            @Override
            public void recover(
                    io.github.kxyr0.echostoryengine.story.api.StoryExecutionState state,
                    ExternalCommand command) {
                if (!command.actionExecutionId().equals(expectedActionId)) {
                    throw new AssertionError("recovery must use the same ActionExecutionId");
                }
            }
        };
    }

    private static void installCatalog(
            StoryCatalogState catalogState,
            ForgeRuntimeDiagnostics diagnostics,
            StoryClock clock) {
        StoryResourceCandidate candidate = compiler(diagnostics).compile(List.of(new StoryResourceText(
                "sample:echo_story/stories/gametest.es",
                storySource())), List.of(), clock.now());
        catalogState.install(candidate, clock.now());
    }

    private static StoryResourceCandidateCompiler compiler(ForgeRuntimeDiagnostics diagnostics) {
        return new StoryResourceCandidateCompiler(new EchoScriptStorySourceCompiler(), diagnostics);
    }

    private static Path root(MinecraftServer server, String storageName) {
        return server.getWorldPath(LevelResource.ROOT)
                .resolve("data")
                .resolve(EchoStoryEngineMod.MOD_ID)
                .resolve("gametest-executions")
                .resolve(storageName + "-" + Long.toHexString(System.nanoTime()));
    }

    private static String storySource() {
        return """
                namespace EchoStoryEngine.Story.Resources;

                class GameTestStory : StoryResource
                {
                    public override StoryResourceDef Build()
                    {
                        return Story("sample:gametest_story", start: "start");
                    }

                    private void Nodes()
                    {
                        Start("start", next: "action");
                        Action("action", type: "sample:complete", next: "finish");
                        Finish("finish");
                    }
                }
                """;
    }

    record TestContext(
            MinecraftServer server,
            StoryCatalogState catalogState,
            FileExecutionStateRepository repository,
            InMemoryDiagnosticBuffer buffer,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
        ForgeServerHost host(ExternalCommandSink sink) {
            PersistenceWorkQueue queue = new BoundedPersistenceWorkQueue(64, "echo-story-engine-gametest",
                    failure -> buffer.accept(diagnostics.persistenceWorkerFailed(failure.getClass().getSimpleName(),
                            clock.now())));
            return new ForgeServerHost(server, catalogState, repository, queue, buffer, clock, diagnostics, sink);
        }
    }

    record CatalogContext(
            StoryCatalogState catalogState,
            InMemoryDiagnosticBuffer buffer,
            StoryClock clock,
            ForgeRuntimeDiagnostics diagnostics) {
    }
}
