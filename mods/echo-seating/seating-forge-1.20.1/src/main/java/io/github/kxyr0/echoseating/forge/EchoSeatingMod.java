package io.github.kxyr0.echoseating.forge;

import io.github.kxyr0.echoseating.forge.api.SeatingServiceLookupEvent;
import io.github.kxyr0.echoseating.forge.command.SeatingCommandRegistration;
import io.github.kxyr0.echoseating.forge.runtime.ForgeSeatingServices;
import io.github.kxyr0.echoseating.forge.runtime.PlayerRidingObserver;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoSeatingMod.MOD_ID)
public final class EchoSeatingMod {
    public static final String MOD_ID = "echo_seating";
    private final ForgeSeatingServices services = new ForgeSeatingServices();
    private final PlayerRidingObserver observer = new PlayerRidingObserver(services);

    public EchoSeatingMod() {
        MinecraftForge.EVENT_BUS.register(observer);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServiceLookup);
    }

    private void onServerStarted(ServerStartedEvent event) {
        services.useServer(event.getServer());
        services.reload(event.getServer().getWorldPath(LevelResource.ROOT));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        SeatingCommandRegistration.register(event.getDispatcher(), services);
    }

    private void onServiceLookup(SeatingServiceLookupEvent event) {
        services.useServer(event.server());
        event.provide(services.bridge());
    }
}
