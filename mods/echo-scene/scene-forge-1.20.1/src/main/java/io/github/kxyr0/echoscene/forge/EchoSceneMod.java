package io.github.kxyr0.echoscene.forge;

import io.github.kxyr0.echoscene.forge.api.EchoSceneCancelEvent;
import io.github.kxyr0.echoscene.forge.api.EchoSceneLaunchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EchoSceneMod.MOD_ID)
public final class EchoSceneMod {
    public static final String MOD_ID = "echo_scene";
    private final SceneForgeCatalog catalog = new SceneForgeCatalog();
    private SceneForgeRuntime runtime = new SceneForgeRuntime(catalog);

    public EchoSceneMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void reloadListeners(AddReloadListenerEvent event) {
        event.addListener(catalog.listener());
    }

    @SubscribeEvent
    public void commands(RegisterCommandsEvent event) {
        SceneCommands.register(event.getDispatcher(), catalog, runtime);
    }

    @SubscribeEvent
    public void launch(EchoSceneLaunchEvent event) {
        catalog.find(event.resourceId()).filter(value -> value.successful()).flatMap(value -> value.definition())
                                .map(value -> event.execution().isPresent()
                        ? runtime.play(value, event.player(), event.execution().get(), event.rootExecution().orElseThrow())
                        : runtime.play(value, event.player())).ifPresent(event::provide);
    }

    @SubscribeEvent
    public void cancel(EchoSceneCancelEvent event) {
        runtime.inspect(event.executionId()).map(ignored -> runtime.cancel(event.executionId())).ifPresent(event::provide);
    }

    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
    public void serverStarted(net.minecraftforge.event.server.ServerStartedEvent event) { runtime.initialize(event.getServer()); }

    @SubscribeEvent
    public void playerJoined(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) runtime.recoverPlayer(player);
    }

    @SubscribeEvent
    public void stopped(net.minecraftforge.event.server.ServerStoppedEvent event) { runtime = new SceneForgeRuntime(catalog); }

    @SubscribeEvent
    public void audioLocation(io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent event) {
        catalog.audioLocations(event.reference()).forEach(location -> event.provide(
                new io.github.kxyr0.echoaudio.forge.server.AudioLocationLookupEvent.Position(
                        location.dimension(), location.x(), location.y(), location.z())));
    }

    @SubscribeEvent
    public void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) runtime.tick();
    }
}
