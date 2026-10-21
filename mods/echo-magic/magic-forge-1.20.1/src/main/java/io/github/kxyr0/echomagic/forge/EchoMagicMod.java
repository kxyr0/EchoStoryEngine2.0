package io.github.kxyr0.echomagic.forge;

import io.github.kxyr0.echomagic.forge.api.lookup.EchoMagicServiceLookupEvent;
import io.github.kxyr0.echomagic.forge.client.MagicClientBootstrap;
import io.github.kxyr0.echomagic.forge.command.MagicCommands;
import io.github.kxyr0.echomagic.forge.config.MagicClientConfig;
import io.github.kxyr0.echomagic.forge.config.MagicServerConfig;
import io.github.kxyr0.echomagic.forge.content.ForgeSpellContentLoader;
import io.github.kxyr0.echomagic.forge.content.MagicResourceReloadListener;
import io.github.kxyr0.echomagic.forge.content.MagicSpellHotReload;
import io.github.kxyr0.echomagic.forge.entity.MagicProjectileEntities;
import io.github.kxyr0.echomagic.forge.network.MagicNetwork;
import io.github.kxyr0.echomagic.forge.service.ForgeMagicServices;
import io.github.kxyr0.echomagic.forge.time.MagicTimeDilation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod(EchoMagicMod.MOD_ID)
public final class EchoMagicMod {
    public static final String MOD_ID = "echo_magic";
    private static final ForgeMagicServices SERVICES = new ForgeMagicServices();
    private static final MagicTimeDilation TIME_DILATION = new MagicTimeDilation();
    private final ForgeSpellContentLoader loader = new ForgeSpellContentLoader(FMLPaths.GAMEDIR.get());
    private final MagicSpellHotReload hotReload = new MagicSpellHotReload(FMLPaths.GAMEDIR.get(), loader, SERVICES);

    public EchoMagicMod() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, MagicClientConfig.SPEC, "echo-magic-client.toml");
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, MagicServerConfig.SPEC, "echo-magic-server.toml");
        MagicProjectileEntities.register(modBus);
        MagicNetwork.register();
        SERVICES.accept(loader.loadExternal());
        MinecraftForge.EVENT_BUS.addListener(this::reloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::commands);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::livingTick);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogin);
        MinecraftForge.EVENT_BUS.addListener(this::playerLogout);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::lookup);
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> () -> MagicClientBootstrap.bootstrap(modBus));
    }

    public static ForgeMagicServices services() { return SERVICES; }
    private void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(new MagicResourceReloadListener(loader, SERVICES));
    }
    private void commands(RegisterCommandsEvent event) { new MagicCommands(SERVICES).register(event.getDispatcher()); }
    private void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) TIME_DILATION.tick(event.getServer());
        else {
            SERVICES.tick();
            hotReload.tick(event.getServer(), SERVICES.serverTick());
        }
    }
    private void livingTick(LivingEvent.LivingTickEvent event) { TIME_DILATION.livingTick(event); }
    private void playerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) SERVICES.restoreAndSync(player);
    }
    private void playerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SERVICES.playerLeft(player);
            TIME_DILATION.playerLeft(player);
        }
    }
    public static void drawingState(ServerPlayer player, boolean active) { TIME_DILATION.update(player, active); }
    private void serverStarted(ServerStartedEvent event) { SERVICES.serverStarted(); hotReload.serverStarted(); }
    private void lookup(EchoMagicServiceLookupEvent event) { event.provide(SERVICES); }
}
