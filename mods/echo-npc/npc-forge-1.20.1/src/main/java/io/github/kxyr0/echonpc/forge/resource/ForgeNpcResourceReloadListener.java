package io.github.kxyr0.echonpc.forge.resource;

import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Atomic server-resource reload listener for Echo NPC .npc.es resources.
 */
public final class ForgeNpcResourceReloadListener
        extends SimplePreparableReloadListener<NpcResourceDiscovery.DiscoveredResources> {
    private final NpcResourceDiscovery discovery;
    private final NpcCatalogCompiler compiler;
    private final NpcCatalogState state;
    private final DiagnosticSink diagnostics;
    private final NpcReloadChatNotifier chatNotifier;
    private final Supplier<Optional<MinecraftServer>> server;

    public ForgeNpcResourceReloadListener(
            NpcResourceDiscovery discovery,
            NpcCatalogCompiler compiler,
            NpcCatalogState state,
            DiagnosticSink diagnostics,
            Supplier<Optional<MinecraftServer>> server
    ) {
        this.discovery = java.util.Objects.requireNonNull(discovery, "discovery");
        this.compiler = java.util.Objects.requireNonNull(compiler, "compiler");
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
        this.chatNotifier = new NpcReloadChatNotifier();
        this.server = java.util.Objects.requireNonNull(server, "server");
    }

    @Override
    protected NpcResourceDiscovery.DiscoveredResources prepare(ResourceManager manager, ProfilerFiller profiler) {
        return discovery.discoverAll(manager, diagnostics);
    }

    @Override
    protected void apply(NpcResourceDiscovery.DiscoveredResources discovered,
            ResourceManager manager, ProfilerFiller profiler) {
        long nextRevision = state.current().revision() + 1L;
        NpcCatalogCompiler.Result result = compiler.compile(discovered.npcs(), discovered.forms(), nextRevision);
        List<NpcDiagnostic> reloadDiagnostics = new ArrayList<>(result.diagnostics());
        reloadDiagnostics.forEach(diagnostics::accept);
        if (result.catalog().isEmpty()) {
            diagnostics.accept(NpcDiagnostic.error("ENPC-CATALOG-REJECTED",
                    "Candidate catalog отклонён; previous known-good catalog сохранён.", "npcs="
                            + discovered.npcs().size() + " forms=" + discovered.forms().size()));
            chatNotifier.notifyErrors(server.get(), reloadDiagnostics);
            return;
        }
        state.install(result.catalog().orElseThrow());
        chatNotifier.notifyErrors(server.get(), reloadDiagnostics);
        diagnostics.accept(NpcDiagnostic.info("ENPC-CATALOG-ACCEPTED",
                "Catalog принят атомарно.", "definitions=" + result.catalog().orElseThrow().definitions().size()
                        + " forms=" + result.catalog().orElseThrow().forms().size()));
    }
}
