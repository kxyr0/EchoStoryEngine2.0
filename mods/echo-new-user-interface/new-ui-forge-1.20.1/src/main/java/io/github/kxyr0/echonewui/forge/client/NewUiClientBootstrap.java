package io.github.kxyr0.echonewui.forge.client;

import io.github.kxyr0.echonewui.forge.EchoNewUserInterfaceMod;
import io.github.kxyr0.echonewui.forge.command.NewUiClientCommands;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import io.github.kxyr0.echonewui.forge.hud.CleanHudSuppressionPolicy;
import io.github.kxyr0.echonewui.forge.overlay.JournalBookOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class NewUiClientBootstrap {
    public static final ResourceLocation OVERLAY_ID =
            new ResourceLocation(EchoNewUserInterfaceMod.MOD_ID, "journal_book");
    private static final JournalBookPreviewState PREVIEW_STATE = new JournalBookPreviewState();
    private static final NewUiDiagnostics DIAGNOSTICS =
            new NewUiDiagnostics(EchoNewUserInterfaceMod.LOGGER);
    private static final JournalBookOverlay OVERLAY =
            new JournalBookOverlay(PREVIEW_STATE, DIAGNOSTICS);
    private static final CleanHudSuppressionPolicy HUD_SUPPRESSION =
            new CleanHudSuppressionPolicy(PREVIEW_STATE, OVERLAY_ID);

    private NewUiClientBootstrap() {
    }

    public static void bootstrap(IEventBus modBus) {
        modBus.addListener(NewUiClientBootstrap::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(NewUiClientBootstrap::registerClientCommands);
        MinecraftForge.EVENT_BUS.addListener(HUD_SUPPRESSION::onPreRender);
        EchoNewUserInterfaceMod.LOGGER.info("Echo New UI client bootstrap complete overlay={}", OVERLAY_ID);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("journal_book", OVERLAY::render);
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        NewUiClientCommands.register(event, PREVIEW_STATE, DIAGNOSTICS, OVERLAY_ID);
    }
}
