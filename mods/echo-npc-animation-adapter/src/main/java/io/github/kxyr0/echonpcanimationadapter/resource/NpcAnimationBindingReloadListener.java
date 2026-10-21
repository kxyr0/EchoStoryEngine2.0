package io.github.kxyr0.echonpcanimationadapter.resource;

import com.mojang.logging.LogUtils;
import java.nio.file.Path;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

/**
 * Forge reload listener for adapter binding resources.
 */
public final class NpcAnimationBindingReloadListener
        extends SimplePreparableReloadListener<NpcAnimationBindingCatalog> {
    private static final Logger LOGGER = LogUtils.getLogger();

    private final NpcAnimationBindingDiscovery discovery;
    private final NpcAnimationBindingCompiler compiler;
    private final NpcAnimationBindingCatalogState catalogState;
    private final Path externalRoot;

    public NpcAnimationBindingReloadListener(
            NpcAnimationBindingDiscovery discovery,
            NpcAnimationBindingCompiler compiler,
            NpcAnimationBindingCatalogState catalogState,
            Path externalRoot
    ) {
        this.discovery = discovery;
        this.compiler = compiler;
        this.catalogState = catalogState;
        this.externalRoot = externalRoot;
    }

    @Override
    public String getName() {
        return "echo_npc_animation_bindings";
    }

    @Override
    protected NpcAnimationBindingCatalog prepare(ResourceManager manager, ProfilerFiller profiler) {
        return compiler.compile(discovery.discover(manager, externalRoot));
    }

    @Override
    protected void apply(NpcAnimationBindingCatalog catalog, ResourceManager manager, ProfilerFiller profiler) {
        catalogState.install(catalog);
        LOGGER.info("Echo NPC Animation bindings installed: bindings={}, diagnostics={}",
                catalog.bindings().size(), catalog.diagnostics().size());
        catalog.diagnostics().forEach(diagnostic -> LOGGER.error("[{}] {}", "EANIM-A4-BINDING", diagnostic));
        if (!catalog.valid()) {
            LOGGER.warn("[EANIM-A4-BINDING-CATALOG-RETAINED] Previous adapter binding catalog retained.");
        }
    }
}
