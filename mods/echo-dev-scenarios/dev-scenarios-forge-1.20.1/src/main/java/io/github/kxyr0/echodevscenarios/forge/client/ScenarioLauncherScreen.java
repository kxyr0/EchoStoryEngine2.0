package io.github.kxyr0.echodevscenarios.forge.client;

import io.github.kxyr0.echodevscenarios.forge.network.OpenScenarioScreenMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ScenarioLauncherScreen extends Screen {
    private final List<OpenScenarioScreenMessage.Entry> entries;
    private final List<Button> rows = new ArrayList<>();
    private EditBox search;
    private String previousQuery = "";

    private ScenarioLauncherScreen(OpenScenarioScreenMessage message) {
        super(Component.literal("Echo Scenario Launcher"));
        entries = message.entries();
    }

    public static void open(OpenScenarioScreenMessage message) {
        Minecraft.getInstance().setScreen(new ScenarioLauncherScreen(message));
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(420, width - 40);
        search = new EditBox(font, (width - panelWidth) / 2, 42, panelWidth, 20, Component.literal("Search"));
        search.setHint(Component.literal("Search scenario, title, or target..."));
        addRenderableWidget(search);
        rebuildRows();
    }

    @Override
    public void tick() {
        super.tick();
        search.tick();
        if (!search.getValue().equals(previousQuery)) rebuildRows();
    }

    private void rebuildRows() {
        rows.forEach(this::removeWidget);
        rows.clear();
        previousQuery = search.getValue();
        String query = previousQuery.toLowerCase(Locale.ROOT);
        int panelWidth = Math.min(420, width - 40);
        int x = (width - panelWidth) / 2;
        List<OpenScenarioScreenMessage.Entry> filtered = entries.stream().filter(value -> query.isBlank()
                || value.id().contains(query) || value.title().toLowerCase(Locale.ROOT).contains(query)
                || value.target().contains(query)).limit(8).toList();
        for (int index = 0; index < filtered.size(); index++) {
            OpenScenarioScreenMessage.Entry entry = filtered.get(index);
            String group = entry.groups().isEmpty() ? "" : String.join(" / ", entry.groups()) + " · ";
            Component label = Component.literal(group + entry.title() + "  →  " + entry.target());
            Button button = Button.builder(label, ignored -> play(entry.id()))
                    .bounds(x, 72 + index * 24, panelWidth, 20).build();
            rows.add(addRenderableWidget(button));
        }
    }

    private void play(String id) {
        if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
            minecraft.player.connection.sendCommand("echo_play " + id);
            onClose();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 18, 0xFFFFFF);
        graphics.drawString(font, "VALID scenarios: " + entries.size(), (width - Math.min(420, width - 40)) / 2,
                height - 28, 0xA0FFA0, false);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
