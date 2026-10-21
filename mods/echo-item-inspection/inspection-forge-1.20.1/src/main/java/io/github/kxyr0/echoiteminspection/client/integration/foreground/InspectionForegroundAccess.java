package io.github.kxyr0.echoiteminspection.client.integration.foreground;

import io.github.kxyr0.echoiteminspection.client.world.WorldInspectionPermissionEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModList;

/** Existing screens/camera ownership first, optional typed mod checks second. */
public final class InspectionForegroundAccess {
    private InspectionForegroundAccess() { }

    public static boolean allowed() {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null
                || minecraft.getOverlay() != null || minecraft.options.hideGui
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || minecraft.getCameraEntity() != minecraft.player) return false;
        var mods = ModList.get();
        if (mods.isLoaded("echo_concentration") && ConcentrationAccess.active()) return false;
        if (mods.isLoaded("echo_cutscene") && CutsceneAccess.active()) return false;
        if (mods.isLoaded("echo_dialogue") && DialogueAccess.active()) return false;
        return !MinecraftForge.EVENT_BUS.post(new WorldInspectionPermissionEvent());
    }
}
