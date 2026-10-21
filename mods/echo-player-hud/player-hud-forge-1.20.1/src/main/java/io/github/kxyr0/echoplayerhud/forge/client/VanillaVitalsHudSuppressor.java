package io.github.kxyr0.echoplayerhud.forge.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;

/**
 * Hides vanilla hearts and hunger icons while custom bars are active.
 */
public final class VanillaVitalsHudSuppressor {
    private VanillaVitalsHudSuppressor() {
    }

    public static void onPreRender(RenderGuiOverlayEvent.Pre event) {
        if (event.getOverlay().id().equals(VanillaGuiOverlay.EXPERIENCE_BAR.id())
                || survivalHudActive() && (event.getOverlay().id().equals(VanillaGuiOverlay.PLAYER_HEALTH.id())
                || event.getOverlay().id().equals(VanillaGuiOverlay.FOOD_LEVEL.id()))) {
            event.setCanceled(true);
        }
    }

    private static boolean survivalHudActive() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.gameMode != null && minecraft.gameMode.getPlayerMode() == GameType.SURVIVAL;
    }
}
