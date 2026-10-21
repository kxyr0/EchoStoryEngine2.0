package io.github.kxyr0.echodialoguenpcadapter.acceptance;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.forge.api.SpeakerPresentationProviderCreationContext;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderDescriptor;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echodialoguenpcadapter.EchoDialogueNpcAdapterMod;
import io.github.kxyr0.echodialoguenpcadapter.presentation.NpcSpeakerPresentationProviderFactory;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import java.util.Optional;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

@Mod(N4AcceptanceHarnessMod.MOD_ID)
public final class N4AcceptanceHarnessMod {
    public static final String MOD_ID = "echo_dialogue_npc_adapter_acceptance";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String AUTO_MODE = System.getProperty("echoDialogueNpcAdapterAcceptance.autoMode", "");
    private static final ActorId ACTOR_ID = new ActorId("jdh:content_pack_father");
    private static final DialogueId DIALOGUE_ID = DialogueId.parse("jdh:content_pack_greeting");
    private static final int TIMEOUT_TICKS = 900;
    private MinecraftServer server;
    private boolean passed;
    private int ticks;

    public N4AcceptanceHarnessMod() {
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerTick);
    }

    private void onServerStarted(ServerStartedEvent event) {
        if ("dynamic".equals(AUTO_MODE)) {
            server = event.getServer();
            ticks = 0;
            LOGGER.info("EDNA-ACCEPTANCE START actorId={}", ACTOR_ID.value());
        }
    }

    private void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null || passed) {
            return;
        }
        ticks++;
        try {
            tick();
        } catch (RuntimeException exception) {
            fail("exception=" + exception.getClass().getSimpleName() + ":" + exception.getMessage());
        }
    }

    private void tick() {
        if (ticks > TIMEOUT_TICKS) {
            fail("timeout waiting for spawned actor");
            return;
        }
        Optional<NpcActorView> actor = actor();
        if (actor.isEmpty() || actor.orElseThrow().skinId().isEmpty()) {
            return;
        }
        SpeakerPresentationProviderSession session = providerSession();
        ResolvedSpeakerPresentation line = resolved(session, SpeakerPresentationRequest.Kind.LINE);
        ResolvedSpeakerPresentation choice = resolved(session, SpeakerPresentationRequest.Kind.CHOICE);
        SpeakerPresentationResolution missing = session.resolve(request(
                "jdh:n4_missing_actor", SpeakerPresentationRequest.Kind.LINE));
        if (missing.kind() != SpeakerPresentationResolution.Kind.NOT_HANDLED) {
            fail("missing actor was claimed: " + missing.kind());
            return;
        }
        passed = true;
        LOGGER.info("EDNA-ACCEPTANCE PASS dynamic-line-choice actorId={} status={} display=\"{}\" visual={}",
                ACTOR_ID.value(), actor.orElseThrow().status(), line.displayName(),
                choice.visual().orElseThrow().id());
    }

    private Optional<NpcActorView> actor() {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        if (lookup.status() != EchoNpcServiceLookupResult.Status.RESOLVED || lookup.service().isEmpty()) {
            return Optional.empty();
        }
        NpcResults.Query query = lookup.service().orElseThrow().query(ACTOR_ID);
        if (query.status() != NpcResults.QueryStatus.FOUND) {
            return Optional.empty();
        }
        return query.actor();
    }

    private SpeakerPresentationProviderSession providerSession() {
        NpcSpeakerPresentationProviderFactory factory =
                new NpcSpeakerPresentationProviderFactory(EchoDialogueNpcAdapterMod.VERSION);
        SpeakerPresentationProviderDescriptor descriptor = factory.descriptor();
        if (!NpcSpeakerPresentationProviderFactory.PROVIDER_ID.equals(descriptor.id().toString())) {
            throw new IllegalStateException("provider id mismatch: " + descriptor.id());
        }
        return factory.create(new SpeakerPresentationProviderCreationContext(server));
    }

    private ResolvedSpeakerPresentation resolved(
            SpeakerPresentationProviderSession session,
            SpeakerPresentationRequest.Kind kind
    ) {
        SpeakerPresentationResolution resolution = session.resolve(request(ACTOR_ID.value(), kind));
        if (resolution.kind() != SpeakerPresentationResolution.Kind.RESOLVED) {
            throw new IllegalStateException(kind + " resolution mismatch: " + resolution.kind());
        }
        ResolvedSpeakerPresentation presentation = resolution.presentation().orElseThrow();
        if (!"Отец".equals(presentation.displayName())) {
            throw new IllegalStateException(kind + " display mismatch: " + presentation.displayName());
        }
        if (presentation.visual().isEmpty()
                || presentation.visual().orElseThrow().kind() != SpeakerVisualKind.SKIN_HEAD
                || !ACTOR_ID.value().equals(presentation.visual().orElseThrow().id().toString())) {
            throw new IllegalStateException(kind + " visual mismatch: " + presentation.visual());
        }
        return presentation;
    }

    private SpeakerPresentationRequest request(String speakerId, SpeakerPresentationRequest.Kind kind) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("n4-acceptance-" + kind.name().toLowerCase(java.util.Locale.ROOT)),
                DIALOGUE_ID,
                SpeakerId.parse(speakerId),
                new AudienceId("n4-acceptance-audience"),
                kind,
                "Authored fallback",
                Optional.of(SpeakerVisualRef.portrait("jdh:content_pack_father")));
    }

    private void fail(String reason) {
        LOGGER.error("EDNA-ACCEPTANCE FAIL reason={}", reason);
        passed = true;
        if (server != null) {
            server.executeIfPossible(server::stopServer);
        }
    }
}
