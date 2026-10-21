package io.github.kxyr0.echoevents.forge;

import io.github.kxyr0.echoevents.core.journal.EventJournal;
import io.github.kxyr0.echoevents.core.runtime.EventDispatcher;
import io.github.kxyr0.echoevents.forge.api.EchoEventsLookupEvent;
import io.github.kxyr0.echoevents.forge.api.EventReactionDispatchEvent;
import io.github.kxyr0.echoevents.forge.command.EventCommands;
import io.github.kxyr0.echoevents.persistence.FileEventJournalStore;
import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.common.Mod;

@Mod("echo_events")
public final class EchoEventsMod {
    private final Map<MinecraftServer, EventDispatcher> runtimes = new IdentityHashMap<>();
    private final EventCommands commands = new EventCommands();

    public EchoEventsMod() {
        MinecraftForge.EVENT_BUS.addListener(this::start);
        MinecraftForge.EVENT_BUS.addListener(this::stop);
        MinecraftForge.EVENT_BUS.addListener(this::lookup);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, this::tick);
        MinecraftForge.EVENT_BUS.addListener(commands::register);
    }
    private void start(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        var path = server.getWorldPath(LevelResource.ROOT).resolve("echo/events/journal.ee1");
        var journal = new EventJournal(new FileEventJournalStore(path));
        runtimes.put(server, new EventDispatcher(journal, delivery -> {
            var dispatch = new EventReactionDispatchEvent(server, delivery);
            MinecraftForge.EVENT_BUS.post(dispatch);
            return dispatch.acceptance();
        }, 512, 2048));
    }
    private void stop(ServerStoppedEvent event) {
        runtimes.remove(event.getServer());
        commands.clear();
    }
    private void lookup(EchoEventsLookupEvent event) {
        var runtime = runtimes.get(event.server());
        if (runtime != null) event.register(runtime);
    }
    private void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var runtime = runtimes.get(event.getServer());
        if (runtime != null) {
            runtime.tick(event.getServer().getTickCount(), 128);
            commands.watch(event.getServer(), runtime);
        }
    }
}
