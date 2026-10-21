package io.github.kxyr0.echodialogue.forge.resource;

import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogCandidate;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Forge reload listener for Echo Dialogue server data resources.
 */
public final class ForgeDialogueResourceReloadListener
        extends SimplePreparableReloadListener<DialogueCatalogCandidate> {
    private final DialogueResourceDiscovery discovery;
    private final DialogueResourceCandidateCompiler compiler;
    private final DialogueCatalogState catalogState;
    private final ForgeDiagnosticSink diagnosticSink;
    private final DialogueReloadChatNotifier chatNotifier;
    private final Supplier<Optional<MinecraftServer>> server;

    public ForgeDialogueResourceReloadListener(
            DialogueResourceDiscovery discovery,
            DialogueResourceCandidateCompiler compiler,
            DialogueCatalogState catalogState,
            ForgeDiagnosticSink diagnosticSink,
            Supplier<Optional<MinecraftServer>> server
    ) {
        this.discovery = discovery;
        this.compiler = compiler;
        this.catalogState = catalogState;
        this.diagnosticSink = diagnosticSink;
        this.chatNotifier = new DialogueReloadChatNotifier();
        this.server = server;
    }

    @Override
    public String getName() {
        return "echo_dialogue_resources";
    }

    @Override
    protected DialogueCatalogCandidate prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return compiler.compile(discovery.discover(resourceManager));
    }

    @Override
    protected void apply(DialogueCatalogCandidate candidate, ResourceManager resourceManager, ProfilerFiller profiler) {
        var result = catalogState.install(candidate);
        result.diagnostics().forEach(diagnosticSink::accept);
        chatNotifier.notifyErrors(server.get(), result.diagnostics());
    }
}
