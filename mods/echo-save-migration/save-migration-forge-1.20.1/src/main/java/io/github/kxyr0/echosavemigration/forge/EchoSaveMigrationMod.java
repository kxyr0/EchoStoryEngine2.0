package io.github.kxyr0.echosavemigration.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoSaveMigrationMod.MOD_ID)
public final class EchoSaveMigrationMod {
    public static final String MOD_ID = "echo_save_migration";
    private final ForgeMigrationCatalog catalog = new ForgeMigrationCatalog();
    private final ForgeMigrationRuntime runtime = new ForgeMigrationRuntime(catalog);

    public EchoSaveMigrationMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(catalog.listener());
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        SaveMigrationCommands.register(event.getDispatcher(), catalog);
    }

    @SubscribeEvent
    public void serverStarted(ServerStartedEvent event) {
        runtime.migrateOnStart(event.getServer());
    }
    @SubscribeEvent
    public void canonicalResource(io.github.kxyr0.echosavemigration.forge.api.CanonicalResourceLookupEvent event) {
        event.resolve(catalog.canonical(event.requested()));
    }
}
