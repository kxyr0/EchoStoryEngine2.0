package io.github.kxyr0.echodialogue.forge.client.input;

import java.util.function.BooleanSupplier;
import java.util.function.IntPredicate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Opens and closes the invisible choice input screen for active choice presentations.
 */
public final class ClientDialogueChoiceScreenController {
    private final BooleanSupplier active;
    private final IntPredicate keyChoice;
    private final ClientDialogueChoiceScreen.MouseChoice mouseChoice;
    private final ClientDialogueChoiceScreen.OverlayRender overlayRender;

    public ClientDialogueChoiceScreenController(
            BooleanSupplier active,
            IntPredicate keyChoice,
            ClientDialogueChoiceScreen.MouseChoice mouseChoice,
            ClientDialogueChoiceScreen.OverlayRender overlayRender
    ) {
        this.active = active;
        this.keyChoice = keyChoice;
        this.mouseChoice = mouseChoice;
        this.overlayRender = overlayRender;
    }

    public boolean isOpen() {
        return Minecraft.getInstance().screen instanceof ClientDialogueChoiceScreen;
    }

    public boolean dialogueInputAllowed() {
        Screen screen = Minecraft.getInstance().screen;
        return screen == null || screen instanceof ClientDialogueChoiceScreen;
    }

    public void sync() {
        Minecraft minecraft = Minecraft.getInstance();
        if (active.getAsBoolean()) {
            open(minecraft);
        } else if (minecraft.screen instanceof ClientDialogueChoiceScreen) {
            minecraft.setScreen(null);
        }
    }

    public void close() {
        if (Minecraft.getInstance().screen instanceof ClientDialogueChoiceScreen) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    private void open(Minecraft minecraft) {
        if (!(minecraft.screen instanceof ClientDialogueChoiceScreen)) {
            minecraft.setScreen(new ClientDialogueChoiceScreen(active, keyChoice, mouseChoice, overlayRender));
        }
    }
}
