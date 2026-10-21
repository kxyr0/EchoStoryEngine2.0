package io.github.kxyr0.echodebug.forge.client;

import io.github.kxyr0.echodebug.core.selection.TriggerSelectionSnapshot;
import io.github.kxyr0.echodebug.core.shape.TriggerWandShapeMode;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echodebug.forge.network.ServerboundCreateTriggerPacket;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class TriggerCreationScreen extends Screen {
    private final TriggerSelectionSnapshot selection;
    private EditBox namespace;
    private EditBox path;
    private EditBox story;

    public TriggerCreationScreen(TriggerSelectionSnapshot selection) {
        super(Component.literal(selection.shape() == TriggerWandShapeMode.SEAT
                ? "Триггер посадки на сиденье" : "Создание trigger"));
        this.selection = selection;
    }

    @Override
    protected void init() {
        namespace = new EditBox(font, width / 2 - 110, height / 2 - 36, 220, 20, Component.literal("Namespace"));
        namespace.setValue("jdh");
        path = new EditBox(font, width / 2 - 110, height / 2 - 8, 220, 20, Component.literal("Название"));
        addRenderableWidget(namespace);
        addRenderableWidget(path);
        if (seatMode()) {
            story = new EditBox(font, width / 2 - 110, height / 2 + 24, 220, 20,
                    Component.literal("ID истории"));
            story.setMaxLength(256);
            story.setHint(Component.literal("jdh:chapter_1/story"));
            addRenderableWidget(story);
        }
        int buttonY = height / 2 + (seatMode() ? 58 : 24);
        addRenderableWidget(Button.builder(Component.literal("Создать"), button -> submit())
                .bounds(width / 2 - 110, buttonY, 106, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), button -> onClose())
                .bounds(width / 2 + 4, buttonY, 106, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 64, 0xFFFFFF);
        graphics.drawString(font, Component.literal("Namespace"), width / 2 - 110, height / 2 - 48, 0xA0A0A0);
        graphics.drawString(font, Component.literal("Название триггера"),
                width / 2 - 110, height / 2 - 20, 0xA0A0A0);
        if (seatMode()) {
            graphics.drawString(font, Component.literal("История (один запуск на игрока)"),
                    width / 2 - 110, height / 2 + 12, 0xA0A0A0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void submit() {
        if (path.getValue().isBlank() || (seatMode() && story.getValue().isBlank())) {
            return;
        }
        EchoDebugNetwork.sendToServer(new ServerboundCreateTriggerPacket(UUID.randomUUID().toString(),
                selection.revision(), namespace.getValue(), path.getValue(), seatMode() ? story.getValue() : ""));
    }

    private boolean seatMode() {
        return selection.shape() == TriggerWandShapeMode.SEAT;
    }
}
