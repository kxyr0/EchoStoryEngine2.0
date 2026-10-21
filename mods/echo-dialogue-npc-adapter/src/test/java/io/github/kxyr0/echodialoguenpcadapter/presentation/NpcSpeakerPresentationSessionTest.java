package io.github.kxyr0.echodialoguenpcadapter.presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerNameColor;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echodialoguenpcadapter.diagnostic.AdapterDiagnostics;
import io.github.kxyr0.echodialoguenpcadapter.npc.NpcServiceLookup;
import io.github.kxyr0.echonpc.api.appearance.NpcNameColor;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcSkinId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.NpcServiceHealth;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class NpcSpeakerPresentationSessionTest {
    @Test
    void resolvesFoundActorWithSkinHead() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        ResolvedSpeakerPresentation presentation = resolution.presentation().orElseThrow();
        assertEquals("Отец Джон", presentation.displayName());
        assertEquals(SpeakerVisualKind.SKIN_HEAD, presentation.visual().orElseThrow().kind());
        assertEquals("jdh:john_father", presentation.visual().orElseThrow().id().toString());
    }

    @Test
    void keepsAuthoredVisualWhenActorHasNoSkin() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.empty(), NpcActorView.Status.ACTIVE)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        assertEquals(SpeakerVisualKind.PORTRAIT, resolution.presentation().orElseThrow().visual().orElseThrow().kind());
        assertEquals("jdh:authored_father",
                resolution.presentation().orElseThrow().visual().orElseThrow().id().toString());
    }

    @Test
    void resolvesFoundActorNameColor() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE,
                Optional.of(NpcNameColor.parse("#8FD8FF")))));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(Optional.of(SpeakerNameColor.parse("#8FD8FF")),
                resolution.presentation().orElseThrow().nameColor());
    }

    @Test
    void actorNameColorOverridesAuthoredNameColor() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Dynamic Father", Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE,
                Optional.of(NpcNameColor.parse("#0e0e0e")))));

        SpeakerPresentationResolution resolution = session.resolve(requestWithNameColor(
                "jdh:john_father", "#FC9004"));

        assertEquals(Optional.of(SpeakerNameColor.parse("#0e0e0e")),
                resolution.presentation().orElseThrow().nameColor());
    }

    @Test
    void actorWithoutNameColorKeepsAuthoredNameColor() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Dynamic Father", Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE)));

        SpeakerPresentationResolution resolution = session.resolve(requestWithNameColor(
                "jdh:john_father", "#FC9004"));

        assertEquals(Optional.of(SpeakerNameColor.parse("#FC9004")),
                resolution.presentation().orElseThrow().nameColor());
    }

    @Test
    void actorWithoutSkinAndAuthoredVisualReturnsNameOnly() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.empty(), NpcActorView.Status.ACTIVE)));

        SpeakerPresentationResolution resolution = session.resolve(requestWithoutVisual("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        assertEquals("Отец Джон", resolution.presentation().orElseThrow().displayName());
        assertFalse(resolution.presentation().orElseThrow().visual().isPresent());
    }

    @Test
    void dormantActorStillResolvesPresentation() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.of("jdh:john_father"), NpcActorView.Status.DORMANT_CHUNK_UNLOADED)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
    }

    @Test
    void sleepingActorStillResolvesPresentation() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:sasha",
                "Sasha", Optional.of("jdh:sasha"), NpcActorView.Status.NPC_SLEEPING)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:sasha"));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
    }

    @Test
    void choicePromptUsesSameDynamicPresentation() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE)));

        SpeakerPresentationResolution resolution = session.resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.CHOICE));

        assertEquals(SpeakerPresentationResolution.Kind.RESOLVED, resolution.kind());
        assertEquals("Отец Джон", resolution.presentation().orElseThrow().displayName());
    }

    @Test
    void missingActorDoesNotClaimSpeaker() {
        NpcSpeakerPresentationSession session = session(new NpcResults.Query(
                NpcResults.QueryStatus.NOT_FOUND, Optional.empty(), "Не найден."));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:missing"));

        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED, resolution.kind());
        assertTrue(resolution.diagnostic().isEmpty());
    }

    @Test
    void absentDurableActorDoesNotClaimSpeaker() {
        NpcSpeakerPresentationSession session = session(query(actor(
                "jdh:john_father",
                "Отец Джон",
                Optional.of("jdh:john_father"),
                NpcActorView.Status.ACTIVE,
                NpcActorView.DesiredPresence.ABSENT,
                NpcActorView.DefinitionCompatibility.COMPATIBLE)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED, resolution.kind());
    }

    @Test
    void unavailableNpcServiceDoesNotClaimSpeaker() {
        NpcSpeakerPresentationSession session = new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.unavailable("Нет active service."), new AdapterDiagnostics());

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED, resolution.kind());
    }

    @Test
    void duplicateNpcServiceProviderDoesNotClaimSpeaker() {
        NpcSpeakerPresentationSession session = new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.duplicateProvider("Несколько Echo NPC service providers."),
                new AdapterDiagnostics());

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.NOT_HANDLED, resolution.kind());
    }

    @Test
    void stoppedNpcServiceClaimsTemporarilyUnavailable() {
        NpcSpeakerPresentationSession session = session(new NpcResults.Query(
                NpcResults.QueryStatus.SERVICE_STOPPED, Optional.empty(), "Service stopped."));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.TEMPORARILY_UNAVAILABLE, resolution.kind());
        assertTrue(resolution.claimsSpeaker());
    }

    @Test
    void failedNpcQueryClaimsFailure() {
        NpcSpeakerPresentationSession session = session(new NpcResults.Query(
                NpcResults.QueryStatus.FAILURE, Optional.empty(), "Query failed."));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.FAILURE, resolution.kind());
        assertTrue(resolution.claimsSpeaker());
    }

    @Test
    void foundQueryWithoutActorClaimsFailure() {
        NpcSpeakerPresentationSession session = session(new NpcResults.Query(
                NpcResults.QueryStatus.FOUND, Optional.empty(), "Найден."));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.FAILURE, resolution.kind());
        assertTrue(resolution.claimsSpeaker());
    }

    @Test
    void npcQueryExceptionClaimsFailure() {
        NpcSpeakerPresentationSession session = new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.resolved(new ThrowingNpcService()), new AdapterDiagnostics());

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.FAILURE, resolution.kind());
        assertTrue(resolution.claimsSpeaker());
    }

    @Test
    void blockedCorruptActorFailsAndFallsBackThroughDialogue() {
        NpcSpeakerPresentationSession session = session(query(actor("jdh:john_father",
                "Отец Джон", Optional.of("jdh:john_father"), NpcActorView.Status.BLOCKED_CORRUPT_RECORD)));

        SpeakerPresentationResolution resolution = session.resolve(request("jdh:john_father"));

        assertEquals(SpeakerPresentationResolution.Kind.FAILURE, resolution.kind());
        assertTrue(resolution.claimsSpeaker());
    }

    private NpcSpeakerPresentationSession session(NpcResults.Query query) {
        return new NpcSpeakerPresentationSession(
                () -> NpcServiceLookup.resolved(new QueryOnlyNpcService(query)), new AdapterDiagnostics());
    }

    private NpcResults.Query query(NpcActorView actor) {
        return new NpcResults.Query(NpcResults.QueryStatus.FOUND, Optional.of(actor), "Найден.");
    }

    private SpeakerPresentationRequest request(String speakerId) {
        return request(speakerId, SpeakerPresentationRequest.Kind.LINE);
    }

    private SpeakerPresentationRequest request(String speakerId, SpeakerPresentationRequest.Kind kind) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("session-1"),
                DialogueId.parse("jdh:dialogue"),
                SpeakerId.parse(speakerId),
                new AudienceId("player-1"),
                kind,
                "Отец",
                Optional.of(SpeakerVisualRef.portrait("jdh:authored_father")));
    }

    private SpeakerPresentationRequest requestWithoutVisual(String speakerId) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("session-1"),
                DialogueId.parse("jdh:dialogue"),
                SpeakerId.parse(speakerId),
                new AudienceId("player-1"),
                SpeakerPresentationRequest.Kind.LINE,
                "Отец",
                Optional.empty());
    }

    private SpeakerPresentationRequest requestWithNameColor(String speakerId, String nameColor) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("session-1"),
                DialogueId.parse("jdh:dialogue"),
                SpeakerId.parse(speakerId),
                new AudienceId("player-1"),
                SpeakerPresentationRequest.Kind.LINE,
                "Authored Father",
                Optional.of(SpeakerNameColor.parse(nameColor)),
                Optional.of(SpeakerVisualRef.portrait("jdh:authored_father")));
    }

    private NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status
    ) {
        return actor(actorId, displayName, skinId, status,
                NpcActorView.DesiredPresence.ACTIVE, NpcActorView.DefinitionCompatibility.COMPATIBLE,
                Optional.empty());
    }

    private NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status,
            Optional<NpcNameColor> nameColor
    ) {
        return actor(actorId, displayName, skinId, status,
                NpcActorView.DesiredPresence.ACTIVE, NpcActorView.DefinitionCompatibility.COMPATIBLE,
                nameColor);
    }

    private NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status,
            NpcActorView.DesiredPresence desiredPresence,
            NpcActorView.DefinitionCompatibility definitionCompatibility
    ) {
        return actor(actorId, displayName, skinId, status, desiredPresence, definitionCompatibility,
                Optional.empty());
    }

    private NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status,
            NpcActorView.DesiredPresence desiredPresence,
            NpcActorView.DefinitionCompatibility definitionCompatibility,
            Optional<NpcNameColor> nameColor
    ) {
        NpcPosition position = new NpcPosition(0.0D, 64.0D, 0.0D);
        return new NpcActorView(
                new ActorId(actorId),
                new NpcDefinitionId("jdh:father"),
                displayName,
                nameColor,
                skinId.map(NpcSkinId::new),
                new NpcWorldId("minecraft:overworld"),
                position,
                NpcRotation.zero(),
                status,
                desiredPresence,
                definitionCompatibility,
                Optional.of(position),
                Optional.empty(),
                1L,
                1L,
                Optional.empty());
    }

    private static final class QueryOnlyNpcService implements NpcService {
        private final NpcResults.Query query;

        QueryOnlyNpcService(NpcResults.Query query) {
            this.query = query;
        }

        @Override
        public NpcResults.Spawn spawn(SpawnNpcRequest request) {
            throw new UnsupportedOperationException("spawn is outside N4.");
        }

        @Override
        public NpcResults.Query query(ActorId actorId) {
            return query;
        }

        @Override
        public NpcResults.ListActors listActors(int limit) {
            return new NpcResults.ListActors(java.util.List.of(), false);
        }

        @Override
        public NpcResults.Teleport teleport(TeleportNpcRequest request) {
            throw new UnsupportedOperationException("teleport is outside N4.");
        }

        @Override
        public NpcResults.Despawn despawn(ActorId actorId) {
            throw new UnsupportedOperationException("despawn is outside N4.");
        }

        @Override
        public NpcServiceHealth health() {
            return new NpcServiceHealth(true, 1L, 1, 1, false, false);
        }
    }

    private static final class ThrowingNpcService implements NpcService {
        @Override
        public NpcResults.Spawn spawn(SpawnNpcRequest request) {
            throw new UnsupportedOperationException("spawn is outside N4.");
        }

        @Override
        public NpcResults.Query query(ActorId actorId) {
            throw new IllegalStateException("query failed");
        }

        @Override
        public NpcResults.ListActors listActors(int limit) {
            return new NpcResults.ListActors(java.util.List.of(), false);
        }

        @Override
        public NpcResults.Teleport teleport(TeleportNpcRequest request) {
            throw new UnsupportedOperationException("teleport is outside N4.");
        }

        @Override
        public NpcResults.Despawn despawn(ActorId actorId) {
            throw new UnsupportedOperationException("despawn is outside N4.");
        }

        @Override
        public NpcServiceHealth health() {
            return new NpcServiceHealth(true, 1L, 0, 0, false, false);
        }
    }
}
