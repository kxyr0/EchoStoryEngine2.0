package io.github.kxyr0.echodebug.forge.client;

import io.github.kxyr0.echodebug.forge.client.trail.NpcTrailClientState;
import io.github.kxyr0.echodebug.forge.client.catalog.ClientAreaCatalog;
import io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork;
import io.github.kxyr0.echodebug.forge.network.ServerboundSelectionActionPacket;
import io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EchoDebugClientTick {
    private final ClientSelectionMirror selection;
    private final NpcTrailClientState trails;
    private final ClientAreaCatalog catalog;
    private boolean hadLevel;

    public EchoDebugClientTick(
            ClientSelectionMirror selection,
            NpcTrailClientState trails,
            ClientAreaCatalog catalog
    ) {
        this.selection = selection;
        this.trails = trails;
        this.catalog = catalog;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        clearAfterDisconnect(minecraft);
        EchoDebugClient.refreshWandState(minecraft);
        while (clearKeyActive(minecraft) && EchoDebugKeys.CLEAR_NPC_TRAILS.consumeClick()) {
            EchoDebugClient.clearNpcTrailsFromKey();
        }
        if (!wandActive(minecraft)) {
            return;
        }
        while (EchoDebugKeys.CHANGE_SHAPE.consumeClick()) {
            EchoDebugNetwork.sendToServer(ServerboundSelectionActionPacket.changeShape(
                    requestId(), selection.revision(), Screen.hasShiftDown()));
        }
        while (EchoDebugKeys.FINALIZE_SELECTION.consumeClick()) {
            EchoDebugNetwork.sendToServer(ServerboundSelectionActionPacket.finalizeSelection(
                    requestId(), selection.revision()));
        }
    }

    private void clearAfterDisconnect(Minecraft minecraft) {
        if (minecraft.level == null) {
            if (hadLevel) {
                trails.clearAll();
                catalog.clear();
                hadLevel = false;
            }
            EchoDebugClient.refreshWandState(minecraft);
            return;
        }
        hadLevel = true;
    }

    private static boolean clearKeyActive(Minecraft minecraft) {
        return minecraft.screen == null && minecraft.player != null;
    }

    private static boolean wandActive(Minecraft minecraft) {
        return minecraft.screen == null && minecraft.player != null
                && TriggerWandUseGuard.held(minecraft.player);
    }

    private static String requestId() {
        return UUID.randomUUID().toString();
    }
}
