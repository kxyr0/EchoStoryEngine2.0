package io.github.kxyr0.echoiteminspection.client.session;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Keeps the exact parent alive through Forge's GUI layer lifecycle. */
public final class ItemInspectionReturnContext {
    private final Screen parent;
    private final LocalPlayer player;
    private final ClientLevel level;
    private final AbstractContainerMenu menu;

    public ItemInspectionReturnContext(Minecraft minecraft) {
        parent = minecraft.screen;
        player = minecraft.player;
        level = minecraft.level;
        menu = parent instanceof AbstractContainerScreen<?> container ? container.getMenu()
                : player == null ? null : player.containerMenu;
    }

    public boolean isValid(Minecraft minecraft) {
        return player != null && minecraft.player == player && minecraft.level == level
                && player.isAlive() && !player.isRemoved() && player.containerMenu == menu
                && menu != null && menu.stillValid(player);
    }

    public Screen parent() {
        return parent;
    }

    public boolean isPauseScreen() {
        return parent != null && parent.isPauseScreen();
    }

    public void restore(Minecraft minecraft) {
        if (isValid(minecraft)) minecraft.popGuiLayer();
        else minecraft.setScreen(null);
    }
}
