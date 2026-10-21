package io.github.kxyr0.echoobservation.authoring.forge.client.picker;

import io.github.kxyr0.echoobservation.api.identity.ObservationId;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public final class ObservationPickerScreen extends Screen {
    private static final int ROW_HEIGHT = 18;
    private final ObservationPickerModel model;
    private final Consumer<ObservationId> confirm;
    private EditBox search;
    private int firstRow;
    private String lastClicked;
    private long lastClickMillis;

    public ObservationPickerScreen(List<ObservationPickerItem> items, String selectedId,
            Consumer<ObservationId> confirm) {
        super(Component.translatable("screen.echo_observation_authoring.picker.title"));
        model = new ObservationPickerModel(items, selectedId);
        this.confirm = confirm;
    }

    @Override
    protected void init() {
        search = new EditBox(font, 20, 24, Math.max(120, width - 40), 20,
                Component.translatable("screen.echo_observation_authoring.picker.search"));
        search.setHint(Component.translatable("screen.echo_observation_authoring.picker.search"));
        search.setResponder(value -> { model.search(value); firstRow = 0; });
        search.setFocused(true);
        addRenderableWidget(search);
    }

    @Override public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xf20a0b10);
        graphics.drawCenteredString(font, title, width / 2, 8, 0xffffd166);
        int rows = visibleRows();
        List<ObservationPickerItem> values = model.visible();
        firstRow = Math.max(0, Math.min(firstRow, Math.max(0, values.size() - rows)));
        for (int slot = 0; slot < rows && firstRow + slot < values.size(); slot++) {
            drawRow(graphics, values.get(firstRow + slot), 52 + slot * ROW_HEIGHT);
        }
        String footer = values.isEmpty()
                ? Component.translatable("screen.echo_observation_authoring.picker.empty").getString()
                : Component.translatable("screen.echo_observation_authoring.picker.help").getString();
        graphics.drawCenteredString(font, footer, width / 2, height - 16, 0xffaeb7c6);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRow(GuiGraphics graphics, ObservationPickerItem item, int y) {
        boolean selected = model.selected().filter(value -> value.observationId().equals(item.observationId()))
                .isPresent();
        graphics.fill(16, y - 2, width - 16, y + 14, selected ? 0xff303849 : 0x70191c24);
        int status = item.placed() ? 0xff8c96a8 : 0xff73e0a9;
        String text = item.resourceId() + "  |  " + item.declaration() + "  |  " + item.observationId();
        graphics.drawString(font, Component.literal(item.placed() ? "PLACED" : "UNPLACED"), 22, y, status, false);
        graphics.drawString(font, Component.literal(text), 94, y, selected ? 0xffffffff : 0xffccd2dc, false);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) { onClose(); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { accept(); return true; }
        if (keyCode == GLFW.GLFW_KEY_DOWN) { model.move(1); keepSelectedVisible(); return true; }
        if (keyCode == GLFW.GLFW_KEY_UP) { model.move(-1); keepSelectedVisible(); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true;
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return true;
        int slot = (int) Math.floor((mouseY - 50.0D) / ROW_HEIGHT);
        int index = firstRow + slot;
        if (slot < 0 || index < 0 || index >= model.visible().size()) return true;
        String id = model.visible().get(index).observationId();
        model.select(id);
        long now = Util.getMillis();
        if (id.equals(lastClicked) && now - lastClickMillis <= 300L) accept();
        lastClicked = id;
        lastClickMillis = now;
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        firstRow = Math.max(0, firstRow - (int) Math.signum(delta) * 3);
        return true;
    }

    private void keepSelectedVisible() {
        String id = model.selected().map(ObservationPickerItem::observationId).orElse("");
        int index = java.util.stream.IntStream.range(0, model.visible().size())
                .filter(value -> model.visible().get(value).observationId().equals(id)).findFirst().orElse(0);
        if (index < firstRow) firstRow = index;
        if (index >= firstRow + visibleRows()) firstRow = index - visibleRows() + 1;
    }

    private int visibleRows() { return Math.max(1, (height - 78) / ROW_HEIGHT); }
    private void accept() {
        model.selected().ifPresent(item -> confirm.accept(ObservationId.parse(item.observationId())));
        onClose();
    }
}
