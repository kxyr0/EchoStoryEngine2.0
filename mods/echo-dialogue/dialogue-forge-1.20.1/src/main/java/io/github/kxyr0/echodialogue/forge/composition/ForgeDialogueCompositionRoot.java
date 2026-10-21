package io.github.kxyr0.echodialogue.forge.composition;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.echoscript.EchoScriptDialogueCompiler;
import io.github.kxyr0.echodialogue.forge.api.EchoDialogueServiceLookupEvent;
import io.github.kxyr0.echodialogue.forge.api.wait.DialogueExternalWaitDispatchEvent;
import io.github.kxyr0.echodialogue.forge.binding.DialogueLineBinderRegistration;
import io.github.kxyr0.echodialogue.forge.catalog.DialogueCatalogState;
import io.github.kxyr0.echodialogue.forge.command.EchoDialogueCommands;
import io.github.kxyr0.echodialogue.forge.diagnostic.ForgeDiagnosticSink;
import io.github.kxyr0.echodialogue.forge.diagnostic.InMemoryDiagnosticBuffer;
import io.github.kxyr0.echodialogue.forge.host.ForgeDialogueHost;
import io.github.kxyr0.echodialogue.forge.host.wait.ForgeDialogueDelayScheduler;
import io.github.kxyr0.echodialogue.forge.network.DialogueNetwork;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueAdvanceEvent;
import io.github.kxyr0.echodialogue.forge.network.event.ServerboundDialogueChoiceEvent;
import io.github.kxyr0.echodialogue.forge.persistence.ForgePersistenceScheduler;
import io.github.kxyr0.echodialogue.forge.persistence.ForgeWorldPersistenceRoot;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationCatalog;
import io.github.kxyr0.echodialogue.forge.presentation.ForgeSpeakerPresentationRegistration;
import io.github.kxyr0.echodialogue.forge.resource.DialogueResourceCandidateCompiler;
import io.github.kxyr0.echodialogue.forge.resource.DialogueResourceDiscovery;
import io.github.kxyr0.echodialogue.forge.resource.ForgeDialogueResourceReloadListener;
import io.github.kxyr0.echodialogue.persistence.file.FileDialogueSessionRepository;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Composition root for Forge lifecycle objects.
 */
public final class ForgeDialogueCompositionRoot {
    private final DialogueCatalogState catalogState = new DialogueCatalogState();
    private final InMemoryDiagnosticBuffer buffer = new InMemoryDiagnosticBuffer(200);
    private final ForgeDiagnosticSink diagnosticSink = new ForgeDiagnosticSink(buffer);
    private final ForgeDialogueDelayScheduler delays = new ForgeDialogueDelayScheduler();
    private ForgeDialogueHost host;
    private MinecraftServer server;

    public void register(IEventBus modBus, IEventBus forgeBus) {
        java.util.Objects.requireNonNull(modBus, "modBus");
        DialogueNetwork.register();
        forgeBus.addListener(this::onReloadListeners);
        forgeBus.addListener(this::onRegisterCommands);
        forgeBus.addListener(this::onServerStarted);
        forgeBus.addListener(this::onServerStopping);
        forgeBus.addListener(this::onPlayerLoggedIn);
        forgeBus.addListener(this::onPlayerLoggedOut);
        forgeBus.addListener(this::onAdvanceRequest);
        forgeBus.addListener(this::onChoiceRequest);
        forgeBus.addListener(this::onServerTick);
        forgeBus.addListener(this::onExternalWait);
        forgeBus.addListener(this::onServiceLookup);
    }

    private void onServerStarted(ServerStartedEvent event) {
        delays.clear();
        if (host != null) {
            host.shutdown();
        }
        server = event.getServer();
        ForgeSpeakerPresentationCatalog presentationCatalog =
                ForgeSpeakerPresentationRegistration.register(event.getServer(), diagnosticSink);
        host = new ForgeDialogueHost(
                catalogState,
                diagnosticSink,
                new FileDialogueSessionRepository(ForgeWorldPersistenceRoot.sessionsRoot(event.getServer())),
                new ForgePersistenceScheduler("Echo Dialogue Persistence"),
                System::currentTimeMillis,
                this::playerByAudienceId,
                presentationCatalog);
    }

    private void onServerStopping(ServerStoppingEvent event) {
        delays.clear();
        if (host != null) {
            host.shutdown();
        }
        host = null;
        server = null;
    }

    private void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (host != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            host.reattach(player);
        }
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (host != null && event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            host.disconnect(player);
        }
    }

    private void onAdvanceRequest(ServerboundDialogueAdvanceEvent event) {
        if (host != null) {
            host.advance(event.sender(), event.message().sessionId(), event.message().expectedRevision());
        }
    }

    private void onChoiceRequest(ServerboundDialogueChoiceEvent event) {
        if (host != null) {
            host.selectChoice(event.sender(), event.message().sessionId(), event.message().expectedRevision(),
                    event.message().choiceId());
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (host != null && event.phase == TickEvent.Phase.END) {
            delays.tick(request -> host.service().resumeExternal(request));
            host.drainPersistenceCompletions();
        }
    }

    private void onExternalWait(DialogueExternalWaitDispatchEvent event) {
        if (host == null || !ForgeDialogueDelayScheduler.WAIT_TYPE.equals(event.externalWait().waitType())) {
            return;
        }
        if (!delays.schedule(event.session(), event.externalWait())) {
            host.service().cancel(event.session().sessionId());
        }
    }

    private void onServiceLookup(EchoDialogueServiceLookupEvent event) {
        if (host != null && event.server() == server) {
            event.provide(host.service());
        }
    }

    private void onReloadListeners(AddReloadListenerEvent event) {
        DialogueResourceCandidateCompiler resourceCompiler = new DialogueResourceCandidateCompiler(
                new EchoScriptDialogueCompiler(DialogueLineBinderRegistration.collect(diagnosticSink)));
        event.addListener(new ForgeDialogueResourceReloadListener(
                new DialogueResourceDiscovery(),
                resourceCompiler,
                catalogState,
                diagnosticSink,
                () -> Optional.ofNullable(server)));
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        new EchoDialogueCommands(catalogState, buffer, () -> Optional.ofNullable(host))
                .register(event.getDispatcher());
    }

    private Optional<ServerPlayer> playerByAudienceId(AudienceId audienceId) {
        if (server == null) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(server.getPlayerList().getPlayer(UUID.fromString(audienceId.toString())));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
