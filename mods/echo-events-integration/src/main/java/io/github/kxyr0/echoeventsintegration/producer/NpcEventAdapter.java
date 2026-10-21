package io.github.kxyr0.echoeventsintegration.producer;

import io.github.kxyr0.echoevents.api.context.EventAudience;
import io.github.kxyr0.echoevents.api.context.EventSource;
import io.github.kxyr0.echoevents.api.event.EventEnvelope;
import io.github.kxyr0.echoevents.api.event.EventMutationPolicy;
import io.github.kxyr0.echoevents.api.identity.EventIds.ExecutionId;
import io.github.kxyr0.echoevents.api.identity.EventIds.ResourceId;
import io.github.kxyr0.echoevents.api.identity.EventIds.SymbolId;
import io.github.kxyr0.echoevents.api.identity.EventIds.TransitionId;
import io.github.kxyr0.echoevents.api.payload.NpcFact;
import io.github.kxyr0.echoevents.forge.api.EchoEvents;
import io.github.kxyr0.echonavigation.forge.api.lifecycle.NavigationLifecycleEvent;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupEvent;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import java.util.Optional;
import java.util.OptionalLong;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

public final class NpcEventAdapter {
    @SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public void restoreDeath(net.minecraftforge.event.entity.EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof EchoNpcEntity npc)) return;
        var server = npc.getServer();
        if (server == null) return;
        var id = EventEnvelope.identity("echo_npc", new ExecutionId(npc.getUUID().toString()),
                new TransitionId("death"), io.github.kxyr0.echoevents.api.catalog.EventKind.NPC_DIED);
        if (EchoEvents.service(server).journalImage().occurrences().containsKey(id)) {
            event.setCanceled(true);
            npc.discard();
        }
    }

    @SubscribeEvent
    public void arrived(NavigationLifecycleEvent event) {
        if (event.kind() != NavigationLifecycleEvent.Kind.MOVEMENT_ARRIVED) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) return;
        var movement = event.snapshot();
        var lookup = new EchoNpcServiceLookupEvent(server);
        MinecraftForge.EVENT_BUS.post(lookup);
        if (lookup.providers().size() != 1 || lookup.providers().get(0)
                .query(new ActorId(movement.subjectId().value())).actor().isEmpty()) return;
        var execution = new ExecutionId(movement.moveId().value());
        var runtime = EchoEvents.service(server);
        var context = runtime.executionContext(execution).or(() -> runtime.executionContext(
                new ExecutionId(movement.executionId().value()))
                        .map(value -> value.asCaller(new ExecutionId(movement.executionId().value()))));
        var source = new EventSource(new ResourceId(movement.subjectId().value()), execution,
                context.map(value -> value.root()).orElseGet(() -> new ExecutionId(movement.executionId().value())),
                new TransitionId("arrived"), context.map(value -> value.audience()).orElseGet(EventAudience.World::new),
                "echo_navigation", server.getTickCount(), OptionalLong.empty(),
                Long.toString(movement.settingsRevision()), Optional.empty(), context.map(value -> value.caller()));
        var target = context.flatMap(value -> value.targetResource());
        var destination = movement.destination();
        var position = target.isEmpty() ? Optional.of(new NpcFact.Position(destination.dimensionId(),
                destination.blockX(), destination.blockY(), destination.blockZ())) : Optional.<NpcFact.Position>empty();
        var fact = new NpcFact(new SymbolId(movement.subjectId().value()), target, position);
        EchoEvents.publish(server, EventEnvelope.occurrence(source, fact), EventMutationPolicy.NORMAL);
    }

    @SubscribeEvent
    public void died(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof EchoNpcEntity npc) || npc.level().isClientSide()) return;
        var server = npc.getServer();
        if (server == null || npc.actorIdValue().isBlank()) return;
        var execution = new ExecutionId(npc.getUUID().toString());
        var source = new EventSource(new ResourceId(npc.actorIdValue()), execution, execution,
                new TransitionId("death"), new EventAudience.World(), "echo_npc", server.getTickCount(),
                OptionalLong.empty(), "unavailable", Optional.empty());
        EchoEvents.publish(server, EventEnvelope.occurrence(source,
                new NpcFact(new SymbolId(npc.actorIdValue()), Optional.empty())), EventMutationPolicy.NORMAL);
    }
}
