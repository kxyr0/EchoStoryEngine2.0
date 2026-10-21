package io.github.kxyr0.echoquests.forge.resource;

import io.github.kxyr0.echoquests.echoscript.QuestCompileResult;
import io.github.kxyr0.echoquests.echoscript.QuestScriptCompiler;
import io.github.kxyr0.echoquests.echoscript.QuestScriptSource;
import io.github.kxyr0.echoquests.forge.runtime.QuestServerRuntime;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

public final class QuestReloadListener extends SimplePreparableReloadListener<QuestCompileResult> {
    private final QuestResourceDiscovery discovery = new QuestResourceDiscovery();
    private final QuestScriptCompiler compiler = new QuestScriptCompiler();
    private final Logger logger;

    public QuestReloadListener(Logger logger) { this.logger = logger; }

    @Override public String getName() { return "echo_quests_resources"; }

    @Override
    protected QuestCompileResult prepare(ResourceManager resources, ProfilerFiller profiler) {
        var sources = discovery.discover(resources).stream()
                .map(value -> new QuestScriptSource(value.sourceId(), value.text())).toList();
        return compiler.compile(sources);
    }

    @Override
    protected void apply(QuestCompileResult result, ResourceManager resources, ProfilerFiller profiler) {
        result.diagnostics().forEach(value -> logger.warn("[ECHO_QUESTS] {} {}", value.code(), value.message()));
        if (result.successful()) {
            var catalog = result.catalog().orElseThrow();
            if (QuestServerRuntime.instance().install(catalog)) {
                logger.info("[ECHO_QUESTS] Loaded {} quest definitions", catalog.size());
            } else {
                logger.error("[ECHO_QUESTS] Echo Time rejected the schedule; active catalog was preserved");
            }
        } else {
            logger.error("[ECHO_QUESTS] Quest reload rejected; active catalog was preserved");
        }
    }
}
