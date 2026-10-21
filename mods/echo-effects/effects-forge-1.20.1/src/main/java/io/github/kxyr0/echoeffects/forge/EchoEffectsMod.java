package io.github.kxyr0.echoeffects.forge;

import io.github.kxyr0.echoeffects.api.target.EffectTargetRef;
import io.github.kxyr0.echoeffects.forge.api.lookup.EchoEffectServiceLookupEvent;
import io.github.kxyr0.echoeffects.forge.client.EchoEffectsClientBootstrap;
import io.github.kxyr0.echoeffects.forge.command.ForgeEffectCommands;
import io.github.kxyr0.echoeffects.forge.content.ForgeEffectContentLoader;
import io.github.kxyr0.echoeffects.forge.movement.ForgeMovementContribution;
import io.github.kxyr0.echoeffects.forge.timeline.ForgeTimelineEventDispatcher;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod(EchoEffectsMod.MOD_ID)
public final class EchoEffectsMod {
    public static final String MOD_ID = "echo_effects";
    public static final ForgeEffectServices SERVICES = new ForgeEffectServices();
    private final ForgeEffectContentLoader loader = new ForgeEffectContentLoader(FMLPaths.GAMEDIR.get());
    private final ForgeMovementContribution movement = new ForgeMovementContribution();
    private final ForgeTimelineEventDispatcher timeline = new ForgeTimelineEventDispatcher();

    public EchoEffectsMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        SERVICES.accept(loader.load());
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
        MinecraftForge.EVENT_BUS.addListener(this::onPlayerTick);
        DistExecutor.safeRunWhenOn(Dist.CLIENT,
                () -> () -> EchoEffectsClientBootstrap.bootstrap(modBus, SERVICES));
    }

    public static ForgeEffectServices services() {
        return SERVICES;
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SERVICES.accept(loader.load());
        new ForgeEffectCommands(SERVICES, loader).register(event.getDispatcher());
    }

    private void onServiceLookup(EchoEffectServiceLookupEvent event) {
        event.provide(SERVICES);
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        var timelineEvents = SERVICES.onServerTick(event);
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null && !timelineEvents.isEmpty()) {
            SERVICES.addDiagnostics(timeline.dispatch(server, timelineEvents));
        }
    }

    private void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        EffectTargetRef target = EffectTargetRef.player(player.getUUID());
        double multiplier = SERVICES.contributionSnapshot(target).movementSpeedMultiplier();
        movement.apply(player, multiplier);
    }
}
