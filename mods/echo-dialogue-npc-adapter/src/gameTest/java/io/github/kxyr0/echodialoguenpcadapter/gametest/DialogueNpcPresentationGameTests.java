package io.github.kxyr0.echodialoguenpcadapter.gametest;

import com.mojang.logging.LogUtils;
import io.github.kxyr0.echodialogue.api.identity.AudienceId;
import io.github.kxyr0.echodialogue.api.identity.DialogueId;
import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echodialogue.api.identity.SpeakerId;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualKind;
import io.github.kxyr0.echodialogue.api.visual.SpeakerVisualRef;
import io.github.kxyr0.echodialogue.presentation.api.ResolvedSpeakerPresentation;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationProviderSession;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationRequest;
import io.github.kxyr0.echodialogue.presentation.api.SpeakerPresentationResolution;
import io.github.kxyr0.echodialoguenpcadapter.EchoDialogueNpcAdapterMod;
import io.github.kxyr0.echodialoguenpcadapter.presentation.NpcSpeakerPresentationGameTestAccess;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import org.slf4j.Logger;

@GameTestHolder(EchoDialogueNpcAdapterMod.MOD_ID)
public final class DialogueNpcPresentationGameTests {
    private static final Logger LOGGER = LogUtils.getLogger();

    private DialogueNpcPresentationGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dynamicLineUsesNpcPresentation(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.dynamicLineUsesNpcPresentation");
        FakeNpcService service = service(actor("jdh:john_father", "Отец Джон",
                Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE));
        SpeakerPresentationResolution resolution = session(service).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        ResolvedSpeakerPresentation presentation = resolved(helper, resolution);
        helper.assertTrue(presentation.displayName().equals("Отец Джон"), "dynamic name must match");
        helper.assertTrue(presentation.visual().orElseThrow().kind() == SpeakerVisualKind.SKIN_HEAD,
                "dynamic visual must be skin head");
        helper.assertTrue(presentation.visual().orElseThrow().id().toString().equals("jdh:john_father"),
                "dynamic skin id must match");
        helper.assertTrue(service.queryCount == 1, "query count must be bounded");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void dynamicChoiceUsesNpcPresentation(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.dynamicChoiceUsesNpcPresentation");
        FakeNpcService service = service(actor("jdh:john_father", "Отец Джон",
                Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE));
        SpeakerPresentationResolution resolution = session(service).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.CHOICE));
        ResolvedSpeakerPresentation presentation = resolved(helper, resolution);
        helper.assertTrue(presentation.displayName().equals("Отец Джон"), "choice dynamic name must match");
        helper.assertTrue(presentation.visual().orElseThrow().id().toString().equals("jdh:john_father"),
                "choice dynamic skin must match");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void actorWithoutSkinKeepsAuthoredVisual(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.actorWithoutSkinKeepsAuthoredVisual");
        FakeNpcService service = service(actor("jdh:john_father", "Отец Джон",
                Optional.empty(), NpcActorView.Status.ACTIVE));
        ResolvedSpeakerPresentation presentation = resolved(helper, session(service).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE)));
        helper.assertTrue(presentation.displayName().equals("Отец Джон"), "dynamic no-skin name must match");
        helper.assertTrue(presentation.visual().orElseThrow().kind() == SpeakerVisualKind.PORTRAIT,
                "authored visual must stay portrait");
        helper.assertTrue(presentation.visual().orElseThrow().id().toString().equals("jdh:authored_father"),
                "authored visual id must stay");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void missingActorDoesNotClaimSpeaker(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.missingActorDoesNotClaimSpeaker");
        SpeakerPresentationResolution resolution = session(service()).resolve(
                request("jdh:missing", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.NOT_HANDLED,
                "missing actor must not be claimed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void unavailableNpcServiceDoesNotClaimSpeaker(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.unavailableNpcServiceDoesNotClaimSpeaker");
        SpeakerPresentationResolution resolution = NpcSpeakerPresentationGameTestAccess.withoutService().resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.NOT_HANDLED,
                "unavailable service must not claim speaker");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void absentActorDoesNotClaimSpeaker(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.absentActorDoesNotClaimSpeaker");
        FakeNpcService service = service(actor("jdh:john_father", "Отец Джон",
                Optional.of("jdh:john_father"), NpcActorView.Status.ACTIVE,
                NpcActorView.DesiredPresence.ABSENT));
        SpeakerPresentationResolution resolution = session(service).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.NOT_HANDLED,
                "absent actor must not be claimed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void stoppedNpcServiceUsesTemporaryFallback(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.stoppedNpcServiceUsesTemporaryFallback");
        SpeakerPresentationResolution resolution = session(service(NpcResults.QueryStatus.SERVICE_STOPPED)).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.TEMPORARILY_UNAVAILABLE,
                "stopped service must be temporarily unavailable");
        helper.assertTrue(resolution.claimsSpeaker(), "stopped service result must claim for fallback diagnostic");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void failedNpcQueryUsesFailureFallback(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.failedNpcQueryUsesFailureFallback");
        SpeakerPresentationResolution resolution = session(service(NpcResults.QueryStatus.FAILURE)).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.FAILURE,
                "failed query must return failure");
        helper.assertTrue(resolution.claimsSpeaker(), "failed query must claim for fallback diagnostic");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void blockedActorUsesFailureFallback(GameTestHelper helper) {
        mark("DialogueNpcPresentationGameTests.blockedActorUsesFailureFallback");
        FakeNpcService service = service(actor("jdh:john_father", "Отец Джон",
                Optional.of("jdh:john_father"), NpcActorView.Status.BLOCKED_CORRUPT_RECORD));
        SpeakerPresentationResolution resolution = session(service).resolve(
                request("jdh:john_father", SpeakerPresentationRequest.Kind.LINE));
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.FAILURE,
                "blocked corrupt actor must fail precisely");
        helper.assertTrue(resolution.claimsSpeaker(), "known blocked actor must claim for fallback diagnostic");
        helper.succeed();
    }

    private static SpeakerPresentationProviderSession session(FakeNpcService service) {
        return NpcSpeakerPresentationGameTestAccess.withService(service);
    }

    private static SpeakerPresentationRequest request(String speakerId, SpeakerPresentationRequest.Kind kind) {
        return new SpeakerPresentationRequest(
                new DialogueSessionId("n4-game-test"),
                DialogueId.parse("jdh:n4_game_test"),
                SpeakerId.parse(speakerId),
                new AudienceId("game-test-audience"),
                kind,
                "Отец",
                Optional.of(SpeakerVisualRef.portrait("jdh:authored_father")));
    }

    private static ResolvedSpeakerPresentation resolved(
            GameTestHelper helper,
            SpeakerPresentationResolution resolution
    ) {
        helper.assertTrue(resolution.kind() == SpeakerPresentationResolution.Kind.RESOLVED,
                "resolution must be RESOLVED");
        return resolution.presentation().orElseThrow();
    }

    private static FakeNpcService service(NpcActorView... actors) {
        return new FakeNpcService(List.of(actors));
    }

    private static FakeNpcService service(NpcResults.QueryStatus fixedStatus) {
        return new FakeNpcService(fixedStatus);
    }

    private static NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status
    ) {
        return actor(actorId, displayName, skinId, status, NpcActorView.DesiredPresence.ACTIVE);
    }

    private static NpcActorView actor(
            String actorId,
            String displayName,
            Optional<String> skinId,
            NpcActorView.Status status,
            NpcActorView.DesiredPresence desiredPresence
    ) {
        NpcPosition position = new NpcPosition(0.0D, 64.0D, 0.0D);
        return new NpcActorView(
                new ActorId(actorId),
                new NpcDefinitionId("jdh:father"),
                displayName,
                skinId.map(NpcSkinId::new),
                new NpcWorldId("minecraft:overworld"),
                position,
                NpcRotation.zero(),
                status,
                desiredPresence,
                NpcActorView.DefinitionCompatibility.COMPATIBLE,
                Optional.of(position),
                1L,
                1L,
                Optional.empty());
    }

    private static void mark(String testName) {
        LOGGER.info("EDNA-GAMETEST-RUN {}", testName);
    }

    private static final class FakeNpcService implements NpcService {
        private final Map<ActorId, NpcActorView> actors;
        private final Optional<NpcResults.QueryStatus> fixedStatus;
        private int queryCount;

        FakeNpcService(List<NpcActorView> actors) {
            this.actors = actors.stream().collect(
                    java.util.stream.Collectors.toMap(NpcActorView::actorId, actor -> actor));
            this.fixedStatus = Optional.empty();
        }

        FakeNpcService(NpcResults.QueryStatus fixedStatus) {
            this.actors = Map.of();
            this.fixedStatus = Optional.of(fixedStatus);
        }

        @Override
        public NpcResults.Spawn spawn(SpawnNpcRequest request) {
            throw new UnsupportedOperationException("spawn не входит в N4.");
        }

        @Override
        public NpcResults.Query query(ActorId actorId) {
            queryCount++;
            if (fixedStatus.isPresent()) {
                return new NpcResults.Query(fixedStatus.orElseThrow(), Optional.empty(), "Forced GameTest status.");
            }
            NpcActorView actor = actors.get(actorId);
            if (actor == null) {
                return new NpcResults.Query(NpcResults.QueryStatus.NOT_FOUND, Optional.empty(), "Не найден.");
            }
            return new NpcResults.Query(NpcResults.QueryStatus.FOUND, Optional.of(actor), "Найден.");
        }

        @Override
        public NpcResults.ListActors listActors(int limit) {
            return new NpcResults.ListActors(List.copyOf(actors.values()), false);
        }

        @Override
        public NpcResults.Teleport teleport(TeleportNpcRequest request) {
            throw new UnsupportedOperationException("teleport не входит в N4.");
        }

        @Override
        public NpcResults.Despawn despawn(ActorId actorId) {
            throw new UnsupportedOperationException("despawn не входит в N4.");
        }

        @Override
        public NpcServiceHealth health() {
            return new NpcServiceHealth(true, 1L, actors.size(), actors.size(), false, false);
        }
    }
}
