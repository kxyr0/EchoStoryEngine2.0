package io.github.kxyr0.echodebug.forge.client;

import io.github.kxyr0.echodebug.forge.client.catalog.ClientAreaCatalog;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class EchoDebugHudOverlay {
    private final ClientSelectionMirror selection;
    private final ClientAreaCatalog catalog;

    public EchoDebugHudOverlay(ClientSelectionMirror selection, ClientAreaCatalog catalog) {
        this.selection = selection;
        this.catalog = catalog;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !TriggerWandUseGuard.held(minecraft.player)) {
            return;
        }
        EchoDebugClient.refreshWandState(minecraft);
        draw(graphics, minecraft);
    }

    private void draw(GuiGraphics graphics, Minecraft minecraft) {
        int y = 8;
        graphics.drawString(minecraft.font, Component.translatable("hud.echo_debug.trigger_wand.title"),
                8, y, 0x62FF8E);
        selection.snapshot().ifPresentOrElse(snapshot -> {
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.echo_debug.trigger_wand.shape", snapshot.shape()),
                    8, y + 10, 0xFFFFFF);
            graphics.drawString(minecraft.font,
                    Component.translatable("hud.echo_debug.trigger_wand.points",
                            snapshot.points().size(), snapshot.requiredPoints()),
                    8, y + 20, 0xFFFFFF);
        }, () -> graphics.drawString(minecraft.font,
                Component.translatable("hud.echo_debug.trigger_wand.sync"),
                8, y + 10, 0xFFE28A));
        graphics.drawString(minecraft.font,
                Component.translatable("hud.echo_debug.trigger_wand.triggers", catalog.areas().size()),
                8, y + 30, 0xFFFFFF);
        graphics.drawString(minecraft.font,
                Component.translatable("hud.echo_debug.trigger_wand.hint"),
                8, y + 40, 0xB7B7B7);
    }
}
