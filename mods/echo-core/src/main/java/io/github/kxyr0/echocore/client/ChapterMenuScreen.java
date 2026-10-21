package io.github.kxyr0.echocore.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Empty surface for a user-authored FancyMenu layout. */
public final class ChapterMenuScreen extends Screen {
    private final Screen parent;
    public ChapterMenuScreen(Screen parent) { super(Component.empty()); this.parent = parent; }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xFF000000);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
