package io.github.kxyr0.echostorynpcadapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echonpc.api.block.BlockId;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.identity.NpcDefinitionId;
import io.github.kxyr0.echonpc.api.identity.NpcWorldId;
import io.github.kxyr0.echonpc.api.rest.NpcRestAttachmentView;
import io.github.kxyr0.echonpc.api.scene.NpcSceneCommandId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.api.service.NpcServiceHealth;
import io.github.kxyr0.echonpc.api.service.SpawnNpcRequest;
import io.github.kxyr0.echonpc.api.service.TeleportNpcRequest;
import io.github.kxyr0.echonpc.api.spatial.NpcPosition;
import io.github.kxyr0.echonpc.api.spatial.NpcRotation;
import io.github.kxyr0.echostoryengine.story.api.command.ExternalActionResultCategory;
import io.github.kxyr0.echostoryengine.story.api.identity.ActionExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.ExecutionId;
import io.github.kxyr0.echostoryengine.story.api.identity.StoryId;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletion;
import io.github.kxyr0.echostoryengine.story.extension.api.completion.ActionCompletionAcceptance;
import io.github.kxyr0.echostorynpcadapter.action.NpcActionTypes;
import io.github.kxyr0.echostorynpcadapter.observe.ActionObservation;
import io.github.kxyr0.echostorynpcadapter.observe.ObservationBook;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ObservationBookTest {
    @Test
    void spawnMaterializedActorCompletesWithExactOutputs() {
        var service = new FakeNpcService();
        service.actor = Optional.of(materializedActor());
        var completions = new ArrayList<ActionCompletion>();
        var book = new ObservationBook();
        book.register(spawnObservation(completions, "created"));

        book.tick(service);

        assertEquals(0, book.size());
        assertEquals(ExternalActionResultCategory.SUCCESS, completions.get(0).resultCategory());
        assertEquals(Map.of("actorId", "sample:john", "result", "created"), completions.get(0).outputs());
        assertTrue(completions.get(0).outputs().values().stream().noneMatch(value -> value.contains("00000000")));
    }

    @Test
    void dormantActorKeepsObservationWaiting() {
        var service = new FakeNpcService();
        service.actor = Optional.of(materializedActor(NpcActorView.Status.DORMANT_CHUNK_UNLOADED));
        var completions = new ArrayList<ActionCompletion>();
        var book = new ObservationBook();
        book.register(spawnObservation(completions, "created"));

        book.tick(service);

        assertEquals(1, book.size());
        assertEquals(0, completions.size());
    }

    @Test
    void despawnAbsentActorCompletesAsAlreadyAbsent() {
        var service = new FakeNpcService();
        var completions = new ArrayList<ActionCompletion>();
        var book = new ObservationBook();
        book.register(despawnObservation(completions, "already_absent"));

        book.tick(service);

        assertEquals(Map.of("actorId", "sample:john", "result", "already_absent"),
                completions.get(0).outputs());
    }

    @Test
    void sleepCompletesOnlyAtAuthoredBed() {
        var service = new FakeNpcService();
        service.actor = Optional.of(sleepingActor());
        var completions = new ArrayList<ActionCompletion>();
        var book = new ObservationBook();
        book.register(sleepObservation(completions, "sleeping"));

        book.tick(service);

        assertEquals(0, book.size());
        assertEquals(Map.of("actorId", "sample:john", "result", "sleeping"), completions.get(0).outputs());
    }

    @Test
    void duplicateRegistrationDoesNotGrowObservationTracking() {
        var book = new ObservationBook();
        var completions = new ArrayList<ActionCompletion>();
        var observation = spawnObservation(completions, "created");
        assertEquals(ObservationBook.RegisterStatus.ADDED, book.register(observation));
        assertEquals(ObservationBook.RegisterStatus.DUPLICATE, book.register(observation));
        assertEquals(1, book.size());
        book.close();
        assertEquals(0, book.size());
    }

    private ActionObservation spawnObservation(List<ActionCompletion> completions, String result) {
        return new ActionObservation(ActionObservation.Kind.SPAWN, StoryId.of("sample:story"),
                ExecutionId.of("sample:execution"), ActionExecutionId.of("sample:action"),
                NpcActionTypes.SPAWN, mutationId(), new ActorId("sample:john"),
                Optional.of(new NpcDefinitionId("sample:father")),
                Optional.of(new NpcWorldId("minecraft:overworld")),
                Optional.of(new NpcPosition(1.0D, 64.0D, 2.0D)), Optional.of(NpcRotation.zero()),
                Optional.empty(), Optional.empty(),
                result, "payload", completion -> complete(completions, completion));
    }

    private ActionObservation despawnObservation(List<ActionCompletion> completions, String result) {
        return new ActionObservation(ActionObservation.Kind.DESPAWN, StoryId.of("sample:story"),
                ExecutionId.of("sample:execution"), ActionExecutionId.of("sample:action"),
                NpcActionTypes.DESPAWN, mutationId(), new ActorId("sample:john"), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), result,
                "payload",
                completion -> complete(completions, completion));
    }

    private ActionObservation sleepObservation(List<ActionCompletion> completions, String result) {
        return new ActionObservation(ActionObservation.Kind.SLEEP, StoryId.of("sample:story"),
                ExecutionId.of("sample:execution"), ActionExecutionId.of("sample:action"),
                NpcActionTypes.SLEEP_AT, mutationId(), new ActorId("sample:john"), Optional.empty(),
                Optional.of(new NpcWorldId("minecraft:overworld")), Optional.empty(), Optional.empty(),
                Optional.of(bedPosition()), Optional.of(new BlockId("minecraft:red_bed")), result, "payload",
                completion -> complete(completions, completion));
    }

    private ActionCompletionAcceptance complete(List<ActionCompletion> completions, ActionCompletion completion) {
        completions.add(completion);
        return ActionCompletionAcceptance.ACCEPTED;
    }

    private ActorMutationId mutationId() {
        return new ActorMutationId("echo_story_npc_adapter:aaaaaaaa-aaaa-5aaa-aaaa-aaaaaaaaaaaa");
    }

    private NpcActorView materializedActor() {
        return materializedActor(NpcActorView.Status.MATERIALIZED);
    }

    private NpcActorView materializedActor(NpcActorView.Status status) {
        return new NpcActorView(new ActorId("sample:john"), new NpcDefinitionId("sample:father"),
                "Отец", Optional.empty(), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(1.0D, 64.0D, 2.0D), NpcRotation.zero(), status,
                NpcActorView.DesiredPresence.ACTIVE, NpcActorView.DefinitionCompatibility.COMPATIBLE,
                Optional.of(new NpcPosition(1.0D, 64.0D, 2.0D)), 1L, 1L, Optional.empty());
    }

    private NpcActorView sleepingActor() {
        return new NpcActorView(new ActorId("sample:john"), new NpcDefinitionId("sample:father"),
                "Отец", Optional.empty(), Optional.empty(), new NpcWorldId("minecraft:overworld"),
                new NpcPosition(1.0D, 64.0D, 2.0D), NpcRotation.zero(), NpcActorView.Status.NPC_SLEEPING,
                NpcActorView.DesiredPresence.ACTIVE, NpcActorView.DefinitionCompatibility.COMPATIBLE,
                Optional.of(new NpcPosition(1.0D, 64.0D, 2.0D)), Optional.of(restAttachment()), 1L, 1L,
                Optional.empty());
    }

    private NpcRestAttachmentView restAttachment() {
        return new NpcRestAttachmentView(new ActorId("sample:john"), "echo_npc:vanilla_bed",
                new NpcWorldId("minecraft:overworld"), bedPosition(), new BlockId("minecraft:red_bed"),
                bedPosition(), new NpcPosition(10.5D, 64.5625D, 11.5D), 180.0F, "NPC_SLEEPING", 1L,
                "facing=south", true, new NpcSceneCommandId("sample:sleep"));
    }

    private BlockPosition bedPosition() {
        return new BlockPosition(10, 64, 12);
    }

    private static final class FakeNpcService implements NpcService {
        private Optional<NpcActorView> actor = Optional.empty();

        @Override
        public NpcResults.Spawn spawn(SpawnNpcRequest request) {
            return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.empty(), "Не используется.");
        }

        @Override
        public NpcResults.Query query(ActorId actorId) {
            return actor.map(value -> new NpcResults.Query(NpcResults.QueryStatus.FOUND, actor, "Найден."))
                    .orElseGet(() -> new NpcResults.Query(NpcResults.QueryStatus.NOT_FOUND,
                            Optional.empty(), "Не найден."));
        }

        @Override
        public NpcResults.ListActors listActors(int limit) {
            return new NpcResults.ListActors(List.of(), false);
        }

        @Override
        public NpcResults.Teleport teleport(TeleportNpcRequest request) {
            return new NpcResults.Teleport(NpcResults.TeleportStatus.REJECTED, Optional.empty(), "Не используется.");
        }

        @Override
        public NpcResults.Despawn despawn(ActorId actorId) {
            return new NpcResults.Despawn(NpcResults.DespawnStatus.REJECTED, "Не используется.");
        }

        @Override
        public NpcServiceHealth health() {
            return new NpcServiceHealth(true, 1L, 1, 1, 1, 0, 0, false, false, false);
        }
    }
}
