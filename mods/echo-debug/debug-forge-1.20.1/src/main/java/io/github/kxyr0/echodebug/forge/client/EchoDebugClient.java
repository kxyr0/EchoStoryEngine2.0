package io.github.kxyr0.echodebug.forge.client;

import com.mojang.brigadier.Command;
import io.github.kxyr0.echodebug.core.diagnostic.DebugDiagnosticCode;
import io.github.kxyr0.echodebug.core.trail.NpcTrailHealthSnapshot;
import io.github.kxyr0.echodebug.forge.client.catalog.ClientAreaCatalog;
import io.github.kxyr0.echodebug.forge.client.render.TriggerWorldOverlayRenderer;
import io.github.kxyr0.echodebug.forge.client.trail.NpcTrailClientState;
import io.github.kxyr0.echodebug.forge.client.trail.NpcTrailRenderer;
import io.github.kxyr0.echodebug.forge.network.ClientboundNpcTrailPacket;
import io.github.kxyr0.echodebug.forge.network.ClientboundCreationResultPacket;
import io.github.kxyr0.echodebug.forge.network.ClientboundSelectionPacket;
import io.github.kxyr0.echotrigger.api.authoring.catalog.TriggerAreaDefinitionView;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

public final class EchoDebugClient {
    public static final ClientSelectionMirror SELECTION = new ClientSelectionMirror();
    public static final ClientAreaCatalog CATALOG = new ClientAreaCatalog();
    public static final NpcTrailClientState TRAILS = new NpcTrailClientState();
    private static final EchoDebugHudOverlay TRIGGER_WAND_HUD = new EchoDebugHudOverlay(SELECTION, CATALOG);
    private static final int WAND_SYNC_INTERVAL_TICKS = 100;
    private static boolean bootstrapped;
    private static int lastWandSyncTick = -1;

    private EchoDebugClient() {
    }

    public static void bootstrap(IEventBus modBus) {
        if (bootstrapped) {
            return;
        }
        bootstrapped = true;
        modBus.addListener(EchoDebugKeys::register);
        modBus.addListener(EchoDebugClient::registerGuiOverlays);
        MinecraftForge.EVENT_BUS.addListener(EchoDebugClient::registerClientCommands);
        MinecraftForge.EVENT_BUS.register(new EchoDebugClientTick(SELECTION, TRAILS, CATALOG));
        MinecraftForge.EVENT_BUS.register(new TriggerWorldOverlayRenderer(SELECTION, CATALOG));
        MinecraftForge.EVENT_BUS.register(new NpcTrailRenderer(TRAILS));
        io.github.kxyr0.echodebug.forge.EchoDebugMod.LOGGER.info(
                "Echo Debug Trigger Wand client HUD, input and world renderer registered.");
    }

    private static void registerGuiOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("trigger_wand", TRIGGER_WAND_HUD::render);
    }

    static void refreshWandState(Minecraft minecraft) {
        if (minecraft.player == null || minecraft.level == null
                || !io.github.kxyr0.echodebug.forge.wand.TriggerWandUseGuard.held(minecraft.player)) {
            lastWandSyncTick = -1;
            return;
        }
        int tick = minecraft.player.tickCount;
        if (lastWandSyncTick < 0 || tick < lastWandSyncTick
                || tick - lastWandSyncTick >= WAND_SYNC_INTERVAL_TICKS) {
            io.github.kxyr0.echodebug.forge.network.EchoDebugNetwork.sendToServer(
                    new io.github.kxyr0.echodebug.forge.network.catalog.ServerboundAreaCatalogRequestPacket());
            lastWandSyncTick = tick;
        }
    }

    private static void registerClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("echo_debug")
                .then(Commands.literal("trails")
                        .then(Commands.literal("clear")
                                .executes(context -> clearNpcTrails(context.getSource())))
                        .then(Commands.literal("status")
                                .executes(context -> reportNpcTrailStatus(context.getSource()))))
                .then(Commands.literal("gameMenu")
                        .executes(context -> reportGameMenu(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setGameMenu(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setGameMenu(context.getSource(), false)))));
    }

    private static int reportGameMenu(CommandSourceStack source) {
        boolean enabled = Minecraft.getInstance().options.pauseOnLostFocus;
        success(source, "Echo Debug gameMenu " + state(enabled));
        return Command.SINGLE_SUCCESS;
    }

    private static int setGameMenu(CommandSourceStack source, boolean enabled) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.options.pauseOnLostFocus = enabled;
        minecraft.options.save();
        success(source, "Echo Debug gameMenu " + state(enabled));
        return Command.SINGLE_SUCCESS;
    }

    private static int clearNpcTrails(CommandSourceStack source) {
        int count = TRAILS.clearAll();
        source.sendSuccess(() -> clearMessage(count), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int reportNpcTrailStatus(CommandSourceStack source) {
        NpcTrailHealthSnapshot health = TRAILS.health();
        success(source, "Echo Debug trails integration=" + availability(health.integrationAvailable())
                + " packets=" + health.progressSnapshotsReceived()
                + " trails=" + health.trailInstances()
                + " accepted=" + health.pointsAccepted());
        success(source, "Echo Debug trails rejected stale=" + health.staleSnapshotsRejected()
                + " distance=" + health.pointsRejectedByDistance()
                + " interval=" + health.pointsRejectedByInterval()
                + " published=" + health.renderSnapshotsPublished());
        success(source, "Echo Debug trails render calls=" + health.renderCalls()
                + " lastTick=" + health.lastRenderTick()
                + " inDimension=" + health.trailsInCurrentDimension()
                + " culled=" + health.trailsCulled()
                + " segments=" + health.segmentsSubmitted());
        success(source, "Echo Debug trails diag=" + health.lastDiagnostic());
        return Command.SINGLE_SUCCESS;
    }

    public static void applySelection(ClientboundSelectionPacket packet) {
        SELECTION.apply(packet.snapshot());
        Minecraft minecraft = Minecraft.getInstance();
        if (packet.code() != DebugDiagnosticCode.OK && minecraft.player != null) {
            minecraft.player.displayClientMessage(Component.literal(packet.code() + ": " + packet.message()), true);
        }
        if (packet.openCreationScreen() && minecraft.screen == null) {
            minecraft.setScreen(new TriggerCreationScreen(packet.snapshot()));
        }
    }

    public static void applyCreationResult(ClientboundCreationResultPacket packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(message(packet), false);
        }
        if (packet.successful() && minecraft.screen instanceof TriggerCreationScreen) {
            minecraft.setScreen(null);
        }
    }

    public static void applyCatalog(List<TriggerAreaDefinitionView> areas) {
        CATALOG.apply(areas);
    }

    public static void applyNpcTrail(ClientboundNpcTrailPacket packet) {
        TRAILS.accept(packet.event());
    }

    static void clearNpcTrailsFromKey() {
        Minecraft minecraft = Minecraft.getInstance();
        int count = TRAILS.clearAll();
        if (minecraft.player != null) {
            minecraft.player.displayClientMessage(clearMessage(count), true);
        }
    }

    private static Component message(ClientboundCreationResultPacket packet) {
        String prefix = packet.successful() ? "SUCCESS " : "ERROR ";
        String detail = packet.triggerId().isEmpty() ? packet.message() : packet.triggerId();
        return Component.literal(prefix + packet.code() + ": " + detail);
    }

    private static String state(boolean enabled) {
        return enabled ? "on: focus loss opens Game Menu." : "off: focus loss keeps the game view open.";
    }

    private static String availability(boolean available) {
        return available ? "available" : "missing";
    }

    private static Component clearMessage(int count) {
        String cleared = "\u0421\u043b\u0435\u0434\u044b NPC "
                + "\u043e\u0447\u0438\u0449\u0435\u043d\u044b: " + count;
        String text = count > 0 ? cleared
                : "\u0421\u043b\u0435\u0434\u043e\u0432 NPC \u043d\u0435\u0442";
        return Component.literal(text);
    }

    private static void success(CommandSourceStack source, String message) {
        source.sendSuccess(() -> Component.literal(message), false);
    }
}
