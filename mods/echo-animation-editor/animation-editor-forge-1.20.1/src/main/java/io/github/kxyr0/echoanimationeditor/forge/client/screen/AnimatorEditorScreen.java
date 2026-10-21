package io.github.kxyr0.echoanimationeditor.forge.client.screen;

import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorLayout;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorPanelRect;
import io.github.kxyr0.echoanimationeditor.api.viewport.AnimatorViewport;
import io.github.kxyr0.echoanimationeditor.core.layout.AnimatorResponsiveLayout;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorEditorClient;
import io.github.kxyr0.echoanimationeditor.forge.client.AnimatorForgeSession;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class AnimatorEditorScreen extends Screen {
    private final AnimatorEditorClient client;
    private final AnimatorForgeSession session;
    private final AnimatorResponsiveLayout layout = new AnimatorResponsiveLayout();
    private final AnimatorScreenRenderer renderer = new AnimatorScreenRenderer();
    private boolean draggingGizmo;

    public AnimatorEditorScreen(AnimatorEditorClient client, AnimatorForgeSession session) {
        super(Component.literal("Echo Animator Editor"));
        this.client = client;
        this.session = session;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        AnimatorViewport viewport = viewport();
        AnimatorLayout current = layout.layout(viewport);
        renderer.render(graphics, font, session, viewport, current);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void onClose() {
        client.forceClose("screen closed");
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (session.input().freeLook() && session.input().key(keyCode, true)) {
            return true;
        }
        if (AnimatorScreenInputRouter.controlShortcut(session, keyCode, modifiers)) {
            return true;
        }
        if (AnimatorScreenInputRouter.toolKey(keyCode, modifiers).isPresent()) {
            session.selectTool(AnimatorScreenInputRouter.toolKey(keyCode, modifiers).orElseThrow());
            return true;
        }
        if (AnimatorScreenInputRouter.modeKey(keyCode, modifiers).isPresent()) {
            session.selectMode(AnimatorScreenInputRouter.modeKey(keyCode, modifiers).orElseThrow());
            return true;
        }
        return session.input().key(keyCode, true) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return session.input().key(keyCode, false) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
                && AnimatorScreenInputRouter.handleUiClick(renderer, session, mouseX, mouseY)) {
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && overViewport(mouseX, mouseY)) {
            session.input().freeLook(true);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && overViewport(mouseX, mouseY)) {
            draggingGizmo = session.beginGizmo(mouseX, mouseY, guiViewport());
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            session.input().freeLook(false);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingGizmo) {
            draggingGizmo = false;
            session.finishGizmo(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && session.input().freeLook()) {
            session.input().drag(dx, dy);
            return true;
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && draggingGizmo) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (overViewport(mouseX, mouseY)) {
            session.input().wheel(amount);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    private boolean overViewport(double mouseX, double mouseY) {
        return guiViewport().contains((int) Math.round(mouseX), (int) Math.round(mouseY));
    }

    private AnimatorPanelRect guiViewport() {
        AnimatorViewport viewport = viewport();
        return layout.layout(viewport).viewport().scaleToGui(viewport);
    }

    private static AnimatorViewport viewport() {
        Window window = Minecraft.getInstance().getWindow();
        double guiScale = (double) window.getScreenWidth() / window.getGuiScaledWidth();
        return new AnimatorViewport(window.getScreenWidth(), window.getScreenHeight(),
                window.getGuiScaledWidth(), window.getGuiScaledHeight(), guiScale, 0L);
    }
}
