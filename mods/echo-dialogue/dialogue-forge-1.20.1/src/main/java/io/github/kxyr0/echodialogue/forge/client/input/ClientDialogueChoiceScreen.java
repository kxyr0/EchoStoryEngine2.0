package io.github.kxyr0.echodialogue.forge.client.input;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Invisible non-pausing screen that owns mouse/keyboard input while choices are visible.
 */
public final class ClientDialogueChoiceScreen extends Screen {
    private final BooleanSupplier active;
    private final IntPredicate keyChoice;
    private final MouseChoice mouseChoice;
    private final OverlayRender overlayRender;

    public ClientDialogueChoiceScreen(
            BooleanSupplier active,
            IntPredicate keyChoice,
            MouseChoice mouseChoice,
            OverlayRender overlayRender
    ) {
        super(Component.literal("Echo Dialogue Choice"));
        this.active = active;
        this.keyChoice = keyChoice;
        this.mouseChoice = mouseChoice;
        this.overlayRender = overlayRender;
    }

    @Override
    public void tick() {
        if (!active.getAsBoolean() && minecraft != null && minecraft.screen == this) {
            minecraft.setScreen(null);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        overlayRender.render(graphics, width, height);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT || button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            mouseChoice.request(mouseX, mouseY);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        keyChoice.test(keyCode);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @FunctionalInterface
    public interface MouseChoice {
        boolean request(double mouseX, double mouseY);
    }

    @FunctionalInterface
    public interface OverlayRender {
        void render(GuiGraphics graphics, int width, int height);
    }
}
