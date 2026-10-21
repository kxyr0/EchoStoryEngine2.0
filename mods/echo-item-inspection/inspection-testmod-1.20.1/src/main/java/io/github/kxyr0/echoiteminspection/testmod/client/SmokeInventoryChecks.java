package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

final class SmokeInventoryChecks {
    private final SmokeEvidence evidence;
    private InventoryScreen parent;
    private ItemStack original;

    SmokeInventoryChecks(SmokeEvidence evidence) {
        this.evidence = evidence;
    }

    void open(Minecraft minecraft) {
        parent = new InventoryScreen(minecraft.player);
        minecraft.setScreen(parent);
        original = minecraft.player.getInventory().getItem(0).copy();
        Slot slot = parent.getMenu().getSlot(36);
        evidence.check("inventoryRmbCancelled", click(parent, slot, 1));
        evidence.check("inventoryViewerOpened", minecraft.screen instanceof ItemInspectionScreen);
        var viewer = (ItemInspectionScreen) minecraft.screen;
        evidence.check("inventorySourceTyped", viewer.session().sourceKind()
                == io.github.kxyr0.echoiteminspection.client.world.ItemInspectionSourceKind.INVENTORY);
        evidence.check("titleFallsBackToStackName", viewer.textPanel().title().equals(original.getHoverName().getString()));
        evidence.record("generatedInitialOrientation", viewer.session().transform().orientation().toString());
        evidence.record("generatedInitialZoom", viewer.session().transform().zoom());
        evidence.check("inventoryStackUnchanged", ItemStack.matches(original, slot.getItem()));
        evidence.check("inventoryCarriedUnchanged", parent.getMenu().getCarried().isEmpty());
        evidence.check("exactNbtSnapshot", original.save(new CompoundTag()).equals(
                viewer.session().snapshot().copyStack().save(new CompoundTag())));
        evidence.check("sameMenuWhileInspection", minecraft.player.containerMenu == parent.getMenu());
    }

    void returnAndSplit(Minecraft minecraft) {
        ((ItemInspectionScreen) minecraft.screen).keyPressed(256, 0, 0);
        evidence.check("inventoryEscSameInstance", minecraft.screen == parent);
        evidence.check("inventoryAfterFramesUnchanged", ItemStack.matches(original,
                minecraft.player.getInventory().getItem(0)));
        evidence.check("emptySlotRmbUncancelled", !click(parent, parent.getMenu().getSlot(38), 1));
        evidence.check("emptySlotStillEmpty", parent.getMenu().getSlot(38).getItem().isEmpty());
        Slot cobblestone = parent.getMenu().getSlot(37);
        evidence.check("cobblestoneRmbUncancelled", !click(parent, cobblestone, 1));
        evidence.check("cobblestoneVanillaSplit", cobblestone.getItem().getCount() == 32
                && parent.getMenu().getCarried().getCount() == 32);
        evidence.check("cobblestoneViewerClosed", minecraft.screen == parent);
        parent.onClose();
        customBoundaryChecks(minecraft);
    }

    private void customBoundaryChecks(Minecraft minecraft) {
        var normal = new NormalSlotScreen(minecraft.player.getInventory());
        minecraft.setScreen(normal);
        evidence.check("inheritedModdedSlotRmbCancelled", click(normal, normal.getMenu().getSlot(36), 1));
        ((ItemInspectionScreen) minecraft.screen).onClose();
        evidence.check("inheritedModdedSlotSameParent", minecraft.screen == normal);
        var custom = new CustomSlotScreen(minecraft.player.getInventory());
        minecraft.setScreen(custom);
        evidence.check("customSlotRmbUncancelled", !click(custom, custom.getMenu().getSlot(36), 1));
        evidence.check("customSlotOwnHandlerCalled", custom.clicked && minecraft.screen == custom);
        custom.onClose();
        var narrow = new InventoryScreen(minecraft.player);
        minecraft.setScreen(narrow);
        narrow.resize(minecraft, 320, 240);
        boolean wasVisible = narrow.getRecipeBookComponent().isVisible();
        if (!wasVisible) narrow.getRecipeBookComponent().toggleVisibility();
        evidence.check("narrowRecipeBookRmbUncancelled", !click(narrow, narrow.getMenu().getSlot(36), 1));
        evidence.check("narrowRecipeBookNoViewer", minecraft.screen == narrow);
        if (!wasVisible) narrow.getRecipeBookComponent().toggleVisibility();
        narrow.onClose();
    }

    static boolean click(AbstractContainerScreen<?> screen, Slot slot, int button) {
        boolean previous = InspectionLiveSmoke.syntheticInput;
        InspectionLiveSmoke.syntheticInput = true;
        try {
            return dispatchClick(screen, slot, button);
        } finally {
            InspectionLiveSmoke.syntheticInput = previous;
        }
    }

    private static boolean dispatchClick(AbstractContainerScreen<?> screen, Slot slot, int button) {
        double x = screen.getGuiLeft() + slot.x + 8;
        double y = screen.getGuiTop() + slot.y + 8;
        screen.afterMouseAction();
        boolean cancelled = ForgeHooksClient.onScreenMouseClickedPre(screen, x, y, button);
        if (!cancelled) {
            boolean handled = screen.mouseClicked(x, y, button);
            ForgeHooksClient.onScreenMouseClickedPost(screen, x, y, button, handled);
        }
        Screen active = Minecraft.getInstance().screen;
        if (active != null && !ForgeHooksClient.onScreenMouseReleasedPre(active, x, y, button)) {
            boolean handled = active.mouseReleased(x, y, button);
            ForgeHooksClient.onScreenMouseReleasedPost(active, x, y, button, handled);
        }
        return cancelled;
    }

    private static class NormalSlotScreen extends AbstractContainerScreen<InventoryMenu> {
        NormalSlotScreen(Inventory inventory) {
            super(inventory.player.inventoryMenu, inventory, Component.literal("Normal test slots"));
        }

        @Override protected void renderBg(GuiGraphics graphics, float partialTick, int x, int y) { }
    }

    private static final class CustomSlotScreen extends NormalSlotScreen {
        private boolean clicked;

        CustomSlotScreen(Inventory inventory) {
            super(inventory);
        }

        @Override public boolean mouseClicked(double x, double y, int button) {
            clicked = true;
            return true;
        }
    }
}
