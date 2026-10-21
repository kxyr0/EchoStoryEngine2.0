package io.github.kxyr0.echoiteminspection.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.kxyr0.echoiteminspection.client.input.InspectionInput;
import io.github.kxyr0.echoiteminspection.client.render.InspectionRenderer;
import io.github.kxyr0.echoiteminspection.client.render.InspectionViewport;
import io.github.kxyr0.echoiteminspection.client.render.InspectionLayout;
import io.github.kxyr0.echoiteminspection.client.render.InspectionBackground;
import io.github.kxyr0.echoiteminspection.client.session.ItemInspectionSession;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ItemInspectionScreen extends Screen {
    private final ItemInspectionSession session;
    private final InspectionRenderer renderer;
    private final InspectionInput input;
    private final InspectionTextPanel textPanel;
    private InspectionViewport viewport;
    private InspectionLayout layout;

    public ItemInspectionScreen(ItemInspectionSession session) {
        super(Component.translatable("echo_item_inspection.title"));
        this.session = session;
        renderer = new InspectionRenderer(session);
        input = new InspectionInput(session.transform());
        var text = session.definition().text();
        Component itemTitle = text.title().isBlank() ? session.snapshot().copyStack().getHoverName().copy()
                : Component.literal(text.title());
        textPanel = new InspectionTextPanel(itemTitle, text.description());
    }

    @Override protected void init() {
        var window = minecraft.getWindow();
        viewport = new InspectionViewport(window.getWidth(), window.getHeight(),
                window.getGuiScale(), width, height);
        layout = InspectionLayout.resolve(viewport, !textPanel.description().isBlank());
        textPanel.resize(font, layout, viewport.guiScale());
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        InspectionBackground.render(graphics, width, height);
        renderer.render(graphics, viewport, layout);
        textPanel.render(graphics);
    }

    @Override public void tick() {
        if (!session.returnContext().isValid(minecraft)) minecraft.setScreen(null);
    }

    @Override public boolean mouseClicked(double x, double y, int button) {
        if (!textPanel.mouseClicked(x, y, button)) input.press(button);
        return true;
    }

    @Override public boolean mouseReleased(double x, double y, int button) {
        input.release(button);
        textPanel.release();
        return true;
    }

    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        if (!textPanel.mouseDragged(y, button)) input.drag(button, dx, dy, viewport.guiScale());
        return true;
    }

    @Override public boolean mouseScrolled(double x, double y, double amount) {
        input.scroll(amount);
        return true;
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (textPanel.keyPressed(key)) return true;
        if (minecraft.options.keyInventory.isActiveAndMatches(InputConstants.getKey(key, scanCode))) {
            onClose();
            return true;
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override public void onClose() {
        session.returnContext().restore(minecraft);
    }

    @Override public void removed() {
        input.cancelDrag();
        textPanel.release();
        session.close();
    }

    @Override public boolean isPauseScreen() {
        return session.returnContext().isPauseScreen();
    }

    public ItemInspectionSession session() {
        return session;
    }

    public InspectionTextPanel textPanel() { return textPanel; }
}
