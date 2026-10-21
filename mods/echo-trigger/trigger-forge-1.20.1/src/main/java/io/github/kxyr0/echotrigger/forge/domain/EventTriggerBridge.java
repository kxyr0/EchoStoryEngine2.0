package io.github.kxyr0.echotrigger.forge.domain;

import io.github.kxyr0.echoevents.api.delivery.EventReactionPort.Acceptance;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echoevents.forge.api.EventReactionDispatchEvent;
import io.github.kxyr0.echotrigger.api.domain.EventTriggerDefinition;
import io.github.kxyr0.echotrigger.forge.api.domain.TriggerEventActionDispatchEvent;
import io.github.kxyr0.echotrigger.forge.service.ForgeTriggerServices;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class EventTriggerBridge {
    private MinecraftServer server;
    private EventTriggerArchive archive;

    public EventTriggerBridge(ForgeTriggerServices services) { services.onDomainCatalog(this::replace); }
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void start(ServerStartedEvent event) {
        server = event.getServer();
        var root = server.getWorldPath(LevelResource.ROOT).resolve("echo/events/trigger-definitions");
        archive = new EventTriggerArchive(root);
    }
    @SubscribeEvent
    public void stop(ServerStoppedEvent event) { server = null; archive = null; }
    private void replace(List<EventTriggerDefinition> definitions) {
        if (server == null) {
            return;
        }
        definitions.forEach(archive::save);
        var subscriptions = definitions.stream().map(EventTriggerDefinition::subscription).toList();
        EchoEvents.service(server).replaceSubscriptions(subscriptions);
    }
    @SubscribeEvent
    public void react(EventReactionDispatchEvent event) {
        if (event.server() != server || archive == null) {
            return;
        }
        var definition = archive.find(event.delivery().subscription());
        if (definition.isEmpty()) {
            event.resolve(Acceptance.REJECTED);
            return;
        }
        int ordinal = 0;
        for (var action : definition.get().actions()) {
            var dispatch = new TriggerEventActionDispatchEvent(server, event.delivery(), action,
                    event.delivery().executionToken() + ":action:" + ordinal++);
            MinecraftForge.EVENT_BUS.post(dispatch);
            if (dispatch.acceptance() != Acceptance.ACCEPTED) {
                event.resolve(dispatch.acceptance());
                return;
            }
        }
        event.resolve(Acceptance.ACCEPTED);
    }
}
