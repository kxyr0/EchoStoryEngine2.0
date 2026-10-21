package io.github.kxyr0.echomagic.forge.client.screen;

import io.github.kxyr0.echomagic.forge.EchoMagicMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;

public final class MagicSettingsEntryPoint {
    private MagicSettingsEntryPoint() { }

    public static void register() {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new MagicSettingsScreen(parent)));
        MinecraftForge.EVENT_BUS.addListener(MagicSettingsEntryPoint::screenInitialized);
    }

    private static void screenInitialized(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof OptionsScreen options)) return;
        int x = options.width >= 560 ? options.width - 154 : 4;
        event.addListener(Button.builder(net.minecraft.network.chat.Component.translatable(
                        "screen." + EchoMagicMod.MOD_ID + ".settings.button"),
                button -> Minecraft.getInstance().setScreen(new MagicSettingsScreen(options)))
                .bounds(x, options.height - 28, 150, 20).build());
    }
}
