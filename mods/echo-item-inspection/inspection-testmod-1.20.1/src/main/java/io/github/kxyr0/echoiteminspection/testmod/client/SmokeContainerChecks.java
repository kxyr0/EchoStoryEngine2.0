package io.github.kxyr0.echoiteminspection.testmod.client;

import io.github.kxyr0.echoiteminspection.client.screen.ItemInspectionScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;

final class SmokeContainerChecks {
    private final SmokeEvidence evidence;
    private ContainerScreen parent;
    private ItemInspectionScreen invalidatedViewer;
    private volatile AbstractContainerMenu serverMenu;
    private volatile boolean verified;
    private volatile boolean serverMenuUnchanged;

    SmokeContainerChecks(SmokeEvidence evidence) {
        this.evidence = evidence;
    }

    void requestChest(Minecraft minecraft) {
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            var player = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id);
            var chest = new SimpleContainer(27);
            chest.setItem(0, SmokeClientFixture.token());
            player.openMenu(new SimpleMenuProvider((menuId, inventory, entity) ->
                    ChestMenu.threeRows(menuId, inventory, chest), Component.literal("Inspection smoke chest")));
            serverMenu = player.containerMenu;
        });
    }

    boolean chestReady(Minecraft minecraft) {
        return minecraft.screen instanceof ContainerScreen container
                && ItemStack.matches(container.getMenu().getSlot(0).getItem(), SmokeClientFixture.token());
    }

    void inspect(Minecraft minecraft) {
        parent = (ContainerScreen) minecraft.screen;
        var stack = parent.getMenu().getSlot(0).getItem().copy();
        evidence.check("chestRmbCancelled", SmokeInventoryChecks.click(parent,
                parent.getMenu().getSlot(0), 1));
        evidence.check("chestViewerOpened", minecraft.screen instanceof ItemInspectionScreen);
        evidence.check("chestStackUnchanged", ItemStack.matches(stack, parent.getMenu().getSlot(0).getItem()));
    }

    void returnToChest(Minecraft minecraft) {
        ((ItemInspectionScreen) minecraft.screen).keyPressed(256, 0, 0);
        evidence.check("chestEscSameInstance", minecraft.screen == parent);
        evidence.check("chestEscSameMenu", minecraft.player.containerMenu == parent.getMenu());
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() -> {
            serverMenuUnchanged = minecraft.getSingleplayerServer().getPlayerList().getPlayer(id).containerMenu == serverMenu;
            verified = true;
        });
    }

    boolean serverVerified() {
        return verified;
    }

    void invalidateOnServer(Minecraft minecraft) {
        evidence.check("chestNoClosePacket", serverMenuUnchanged);
        evidence.check("chestSecondRmbCancelled", SmokeInventoryChecks.click(parent,
                parent.getMenu().getSlot(0), 1));
        invalidatedViewer = (ItemInspectionScreen) minecraft.screen;
        var id = minecraft.player.getUUID();
        minecraft.getSingleplayerServer().execute(() ->
                minecraft.getSingleplayerServer().getPlayerList().getPlayer(id).closeContainer());
    }

    boolean invalidationArrived(Minecraft minecraft) {
        return minecraft.screen == null && minecraft.player.containerMenu == minecraft.player.inventoryMenu;
    }

    void checkStaleReturn(Minecraft minecraft) {
        invalidatedViewer.onClose();
        evidence.check("invalidParentNotResurrected", minecraft.screen == null
                && minecraft.player.containerMenu == minecraft.player.inventoryMenu);
    }
}
