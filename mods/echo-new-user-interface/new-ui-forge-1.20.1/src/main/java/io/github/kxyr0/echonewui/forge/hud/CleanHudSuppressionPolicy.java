package io.github.kxyr0.echonewui.forge.hud;

import io.github.kxyr0.echonewui.forge.client.JournalBookPreviewState;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

public final class CleanHudSuppressionPolicy {
    private final JournalBookPreviewState state;
    private final ResourceLocation journalBookOverlay;

    public CleanHudSuppressionPolicy(JournalBookPreviewState state, ResourceLocation journalBookOverlay) {
        this.state = state;
        this.journalBookOverlay = journalBookOverlay;
    }

    public void onPreRender(RenderGuiOverlayEvent.Pre event) {
        if (!state.visible()) {
            return;
        }
        if (journalBookOverlay.equals(event.getOverlay().id())) {
            return;
        }
        event.setCanceled(true);
    }
}
