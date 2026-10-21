package io.github.kxyr0.echomagic.forge.client.screen;

import io.github.kxyr0.echomagic.api.gesture.MagicGestureTemplateTarget;
import io.github.kxyr0.echomagic.forge.client.MagicClientBootstrap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MagicHandwritingScreen extends Screen {
    private final Screen parent;

    public MagicHandwritingScreen(Screen parent) {
        super(Component.translatable("screen.echo_magic.handwriting.title")); this.parent = parent;
    }

    @Override protected void init() {
        MagicGestureTemplateTarget[] targets = MagicGestureTemplateTarget.values();
        int rows = (targets.length + 1) / 2;
        int top = Math.max(48, height / 2 - rows * 12 - 12);
        for (int index = 0; index < targets.length; index++) {
            MagicGestureTemplateTarget target = targets[index];
            int column = index % 2; int row = index / 2;
            addRenderableWidget(Button.builder(Component.translatable(target.translationKey()),
                    button -> select(target)).bounds(width / 2 - 154 + column * 158,
                    top + row * 24, 150, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.back"), button -> onClose())
                .bounds(width / 2 - 100, Math.min(height - 28, top + rows * 24 + 6), 200, 20).build());
    }

    private void select(MagicGestureTemplateTarget target) {
        MagicClientBootstrap.beginTraining(target); minecraft.setScreen(null);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 20, 0xFFFFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.echo_magic.handwriting.hint"),
                width / 2, 36, 0xA9A9A9);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
