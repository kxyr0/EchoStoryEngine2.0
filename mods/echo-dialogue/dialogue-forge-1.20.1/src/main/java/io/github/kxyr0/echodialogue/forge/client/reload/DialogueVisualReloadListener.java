package io.github.kxyr0.echodialogue.forge.client.reload;

import io.github.kxyr0.echodialogue.forge.client.ClientDialogueClient;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * Drops cached speaker visuals after a resource reload.
 */
public final class DialogueVisualReloadListener extends SimplePreparableReloadListener<Void> {
    @Override
    protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void value, ResourceManager resourceManager, ProfilerFiller profiler) {
        ClientDialogueClient.clearVisualCache();
    }
}
