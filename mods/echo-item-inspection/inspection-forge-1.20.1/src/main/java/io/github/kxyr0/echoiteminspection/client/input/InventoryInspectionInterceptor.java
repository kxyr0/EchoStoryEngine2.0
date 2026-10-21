package io.github.kxyr0.echoiteminspection.client.input;

import io.github.kxyr0.echoiteminspection.client.api.ItemInspectionClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;

/** Cancels precisely the opening press before vanilla can split or place a stack. */
public final class InventoryInspectionInterceptor {
    private static boolean openingReleasePending;

    private InventoryInspectionInterceptor() { }

    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != 1) return;
        openingReleasePending = false;
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen)) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != screen
                || screen.getMenu() != minecraft.player.containerMenu
                || !InventorySlotBoundary.isNormal(screen)) return;
        Slot slot = InventorySlotBoundary.findSlot(screen, event.getMouseX(), event.getMouseY());
        if (slot == null || !slot.hasItem() || !ItemInspectionClient.isInspectable(slot.getItem())) return;
        if (ItemInspectionClient.openInspection(slot.getItem())) {
            openingReleasePending = true;
            event.setCanceled(true);
        }
    }

    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (event.getButton() != 1 || !openingReleasePending) return;
        openingReleasePending = false;
        event.setCanceled(true);
    }
}
