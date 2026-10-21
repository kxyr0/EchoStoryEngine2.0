package io.github.kxyr0.echoteleportmagic.forge;

import io.github.kxyr0.echoteleportmagic.forge.network.TeleportNetwork;
import io.github.kxyr0.echoteleportmagic.forge.server.TeleportService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoTeleportMagic.ID)
public final class EchoTeleportMagic {
    public static final String ID = "echo_teleport_magic";
    private static final java.util.concurrent.atomic.AtomicReference<TeleportService> SERVICE = new java.util.concurrent.atomic.AtomicReference<>();
    public EchoTeleportMagic() {
        TeleportNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
        if (ModList.get().isLoaded("echo_scene")) MinecraftForge.EVENT_BUS.register(
                new io.github.kxyr0.echoteleportmagic.forge.integration.TeleportSceneBridge());
    }
    public static TeleportService service() {
        if (SERVICE.get() == null) throw new IllegalStateException("Teleport server not started");
        return SERVICE.get();
    }
    @SubscribeEvent public void started(ServerStartedEvent event) { SERVICE.set(new TeleportService(event.getServer())); }
    @SubscribeEvent public void stopping(ServerStoppingEvent event) { if (SERVICE.get() != null) SERVICE.get().stop(); SERVICE.set(null); }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && SERVICE.get() != null) SERVICE.get().tick();
    }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        io.github.kxyr0.echoteleportmagic.forge.command.TeleportCommands.register(event.getDispatcher());
    }
}

