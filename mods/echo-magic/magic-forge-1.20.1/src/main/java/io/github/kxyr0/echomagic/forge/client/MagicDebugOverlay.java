package io.github.kxyr0.echomagic.forge.client;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;

final class MagicDebugOverlay {
    void render(RenderGuiOverlayEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.options.renderDebug) return;
        ClientMagicState state = MagicClientBootstrap.state();
        List<String> lines = List.of("Echo Magic",
                "selected: " + state.selected().orElse("<none>"),
                "gesture school: " + state.gestureSchool().orElse("<none>"),
                "known: " + state.known().size(),
                "cooldown: " + state.cooldown(),
                "particles: " + MagicClientBootstrap.budget().active(),
                "last rejection: " + state.lastRejection());
        for (int index = 0; index < lines.size(); index++) {
            event.getGuiGraphics().drawString(minecraft.font, lines.get(index), 8, 8 + index * 10, 0xD8B8FF, true);
        }
    }
}
