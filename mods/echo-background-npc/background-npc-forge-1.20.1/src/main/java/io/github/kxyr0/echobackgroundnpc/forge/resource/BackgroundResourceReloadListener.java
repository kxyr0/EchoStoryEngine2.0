package io.github.kxyr0.echobackgroundnpc.forge.resource;

import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalog;
import io.github.kxyr0.echobackgroundnpc.core.catalog.BackgroundCatalogHost;
import io.github.kxyr0.echobackgroundnpc.echoscript.BackgroundCompileResult;
import io.github.kxyr0.echobackgroundnpc.echoscript.BackgroundScriptCompiler;
import io.github.kxyr0.echobackgroundnpc.echoscript.BackgroundScriptSource;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/** Atomic LKG resource reload for all four background resource kinds. */
public final class BackgroundResourceReloadListener
        extends SimplePreparableReloadListener<List<BackgroundScriptSource>> {
    private final BackgroundResourceDiscovery discovery = new BackgroundResourceDiscovery();
    private final BackgroundScriptCompiler compiler = new BackgroundScriptCompiler();
    private final BackgroundCatalogHost host;
    private final Consumer<BackgroundCatalog> accepted;
    private final Logger logger;

    public BackgroundResourceReloadListener(BackgroundCatalogHost host, Consumer<BackgroundCatalog> accepted,
            Logger logger) {
        this.host = Objects.requireNonNull(host, "host");
        this.accepted = Objects.requireNonNull(accepted, "accepted");
        this.logger = Objects.requireNonNull(logger, "logger");
    }

    @Override
    protected List<BackgroundScriptSource> prepare(ResourceManager manager, ProfilerFiller profiler) {
        return discovery.discover(manager);
    }

    @Override
    protected void apply(List<BackgroundScriptSource> resources, ResourceManager manager, ProfilerFiller profiler) {
        BackgroundCompileResult result = compiler.compile(resources, host.current().revision() + 1L);
        result.diagnostics().forEach(value -> logger.warn("[BG_NPC] {} {} {}", value.code(), value.message(),
                value.source().map(Object::toString).orElse("<resource>")));
        if (!host.commit(result.catalog(), result.diagnostics())) {
            logger.error("[BG_NPC] Candidate rejected; previous known-good population remains active.");
            return;
        }
        accepted.accept(host.current());
        logger.info("[BG_NPC] Catalog revision {} accepted: agents={}", host.current().revision(),
                host.current().definitions().size());
    }
}
