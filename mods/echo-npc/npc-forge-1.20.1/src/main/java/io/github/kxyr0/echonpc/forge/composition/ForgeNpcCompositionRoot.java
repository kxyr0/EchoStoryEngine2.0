package io.github.kxyr0.echonpc.forge.composition;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.EchoScriptNpcCompiler;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupEvent;
import io.github.kxyr0.echonpc.forge.api.navigation.NpcNavigationBodyLookupEvent;
import io.github.kxyr0.echonpc.forge.command.EchoNpcCommands;
import io.github.kxyr0.echonpc.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echonpc.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.host.ForgeNpcHost;
import io.github.kxyr0.echonpc.forge.registry.EchoNpcEntities;
import io.github.kxyr0.echonpc.forge.resource.ForgeNpcResourceReloadListener;
import io.github.kxyr0.echonpc.forge.resource.NpcCatalogCompiler;
import io.github.kxyr0.echonpc.forge.resource.NpcResourceDiscovery;
import io.github.kxyr0.echonpc.core.definition.NpcCatalogState;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Composition root for Forge lifecycle, resource reload, commands and service lookup.
 */
public final class ForgeNpcCompositionRoot {
    private final NpcCatalogState catalogState = new NpcCatalogState();
    private final InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(200);
    private final ForgeDiagnosticSink diagnostics = new ForgeDiagnosticSink(buffer);
    private final NpcCatalogCompiler catalogCompiler = new NpcCatalogCompiler(new EchoScriptNpcCompiler());
    private ForgeNpcHost host;
    private MinecraftServer server;

    public void register(IEventBus modBus, IEventBus forgeBus) {
        modBus.addListener(this::onEntityAttributes);
        forgeBus.addListener(this::onReloadListeners);
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onServerStopping);
        forgeBus.addListener(this::onServerTick);
        forgeBus.addListener(this::onEntityJoinLevel);
        forgeBus.addListener(this::onEntityLeaveLevel);
        forgeBus.addListener(this::onServiceLookup);
        forgeBus.addListener(this::onNavigationBodyLookup);
    }

    private void onEntityAttributes(EntityAttributeCreationEvent event) {
        event.put(EchoNpcEntities.ECHO_NPC.get(), EchoNpcEntity.createAttributes().build());
    }

    private void onServerStarted(ServerStartedEvent event) {
        if (host != null) {
            host.stop();
        }
        server = event.getServer();
        host = new ForgeNpcHost(server, catalogState, diagnostics);
        host.start();
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (host != null) {
            host.stop();
        }
        host = null;
        server = null;
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (host != null && event.phase == TickEvent.Phase.END) {
            host.tick();
        }
    }

    private void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (host != null && event.getEntity() instanceof EchoNpcEntity entity) {
            host.reconcileLoadedEntity(entity);
        }
    }

    private void onEntityLeaveLevel(EntityLeaveLevelEvent event) {
        if (host == null || !(event.getEntity() instanceof EchoNpcEntity entity)
                || entity.level().isClientSide()
                || entity.getRemovalReason() != net.minecraft.world.entity.Entity.RemovalReason.KILLED) {
            return;
        }
        try {
            ActorId actorId = new ActorId(entity.actorIdValue());
            ActorMutationId mutationId = new ActorMutationId("echo_npc:death/" + entity.getUUID());
            var result = host.despawn(actorId, mutationId);
            diagnostics.accept(NpcDiagnostic.info("ENPC-ACTOR-DEATH-DESPAWN",
                    "Killed NPC scheduled for durable actor removal.",
                    actorId.value() + " status=" + result.status()));
        } catch (IllegalArgumentException exception) {
            diagnostics.accept(NpcDiagnostic.warning("ENPC-ACTOR-DEATH-LINK-INVALID",
                    "Killed Echo NPC had an invalid actor link.", entity.getUUID().toString()));
        }
    }

    private void onServiceLookup(EchoNpcServiceLookupEvent event) {
        if (host != null && event.server() == server) {
            event.provide(host.service());
        }
    }

    private void onNavigationBodyLookup(NpcNavigationBodyLookupEvent event) {
        if (host != null && event.server() == server) {
            host.navigationBody(event.actorId()).ifPresent(event::provide);
        }
    }

    private void onReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ForgeNpcResourceReloadListener(new NpcResourceDiscovery(),
                catalogCompiler, catalogState, diagnostics, () -> Optional.ofNullable(server)));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoNpcCommands(catalogState, buffer, () -> Optional.ofNullable(host))
                .register(event.getDispatcher());
    }
}
