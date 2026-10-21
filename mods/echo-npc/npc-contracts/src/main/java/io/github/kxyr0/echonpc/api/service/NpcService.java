package io.github.kxyr0.echonpc.api.service;

import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeRequest;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeResult;
import io.github.kxyr0.echonpc.api.appearance.NpcSkinChangeStatus;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.identity.ActorMutationId;
import io.github.kxyr0.echonpc.api.form.NpcFormCatalogView;
import io.github.kxyr0.echonpc.api.form.NpcFormId;
import io.github.kxyr0.echonpc.api.form.NpcFormOutcome;
import io.github.kxyr0.echonpc.api.form.NpcFormStatus;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeRequest;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormChangeResult;
import io.github.kxyr0.echonpc.api.form.definition.NpcFormDefinition;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceRequest;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceResult;
import io.github.kxyr0.echonpc.api.scene.NpcPlaceStatus;
import io.github.kxyr0.echonpc.api.rest.NpcRestAttachmentView;
import io.github.kxyr0.echonpc.api.rest.NpcRestQueryService;
import io.github.kxyr0.echonpc.api.rest.NpcRestService;
import io.github.kxyr0.echonpc.api.rest.command.NpcSleepRequest;
import io.github.kxyr0.echonpc.api.rest.command.NpcWakeRequest;
import io.github.kxyr0.echonpc.api.scene.NpcMaterializationService;
import io.github.kxyr0.echonpc.api.scene.NpcSceneReadinessView;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import java.util.List;
import java.util.Optional;

/**
 * Neutral server-scoped Echo NPC service. Implementations are synchronous and server-thread owned in Forge.
 */
public interface NpcService extends NpcMaterializationService, NpcRestService, NpcRestQueryService {
    NpcResults.Spawn spawn(SpawnNpcRequest request);

    NpcResults.Query query(ActorId actorId);

    NpcResults.ListActors listActors(int limit);

    NpcResults.Teleport teleport(TeleportNpcRequest request);

    default NpcPlaceResult placeAt(NpcPlaceRequest request) {
        java.util.Objects.requireNonNull(request, "request");
        NpcResults.Teleport result = teleport(new TeleportNpcRequest(request.npcId(), request.worldId(),
                request.position(), request.rotation()));
        return new NpcPlaceResult(placeStatus(result.status()), result.actor(), result.operationId(),
                result.message());
    }

    default NpcFormChangeResult setForm(NpcFormChangeRequest request) {
        java.util.Objects.requireNonNull(request, "request");
        return new NpcFormChangeResult(NpcFormStatus.REJECTED, NpcFormOutcome.OLD_FORM_RETAINED,
                query(request.npcId()).actor(), "Npc.SetForm не поддерживается этой реализацией.");
    }

    default NpcSkinChangeResult changeSkin(NpcSkinChangeRequest request) {
        java.util.Objects.requireNonNull(request, "request");
        return new NpcSkinChangeResult(NpcSkinChangeStatus.REJECTED, request.skinId(),
                query(request.npcId()).actor(), "Npc.ChangeSkin не поддерживается этой реализацией.");
    }

    NpcResults.Despawn despawn(ActorId actorId);

    default NpcResults.Despawn despawn(ActorId actorId, ActorMutationId mutationId) {
        java.util.Objects.requireNonNull(mutationId, "mutationId");
        return despawn(actorId);
    }

    NpcServiceHealth health();

    default Optional<NpcFormId> getForm(ActorId npcId) {
        return query(npcId).actor().flatMap(NpcActorView::activeFormId);
    }

    default boolean isForm(ActorId npcId, NpcFormId formId) {
        return getForm(npcId).map(formId::equals).orElse(false);
    }

    default NpcFormCatalogView formCatalog(int limit) {
        return new NpcFormCatalogView(0L, List.of(), false);
    }

    default Optional<NpcFormDefinition> form(NpcFormId formId) {
        return formCatalog(200).find(formId);
    }

    @Override
    default NpcResults.Spawn spawnAt(SpawnNpcRequest request) {
        return spawn(request);
    }

    @Override
    default boolean isMaterialized(ActorId npcId) {
        return query(npcId).actor().map(actor -> actor.status() == NpcActorView.Status.MATERIALIZED
                || actor.status() == NpcActorView.Status.NPC_SLEEPING).orElse(false);
    }

    @Override
    default NpcSceneReadinessView sceneReadiness(ActorId npcId) {
        Optional<NpcActorView> actor = query(npcId).actor();
        boolean materialized = actor.map(view -> view.status() == NpcActorView.Status.MATERIALIZED
                || view.status() == NpcActorView.Status.NPC_SLEEPING).orElse(false);
        long revision = actor.map(NpcActorView::actorRevision).orElse(0L);
        String message = materialized ? "NPC materialized in authoritative runtime." : "NPC is not materialized.";
        return new NpcSceneReadinessView(npcId, materialized, revision, "SERVER_RUNTIME_SNAPSHOT", message);
    }

    @Override
    default NpcResults.Sleep sleepAt(NpcSleepRequest request) {
        return new NpcResults.Sleep(NpcResults.SleepStatus.REJECTED, Optional.empty(),
                "Npc.SleepAt не поддерживается этой реализацией.");
    }

    @Override
    default NpcResults.Wake wakeUp(NpcWakeRequest request) {
        return new NpcResults.Wake(NpcResults.WakeStatus.REJECTED, Optional.empty(),
                "Npc.WakeUp не поддерживается этой реализацией.");
    }

    @Override
    default boolean isSleeping(ActorId npcId) {
        return inspectRest(npcId).isPresent();
    }

    @Override
    default boolean isSleepingAt(ActorId npcId, BlockPosition bedPosition) {
        return inspectRest(npcId).map(rest -> rest.canonicalBedPosition().equals(bedPosition)
                || rest.bedPosition().equals(bedPosition)).orElse(false);
    }

    @Override
    default Optional<NpcRestAttachmentView> inspectRest(ActorId npcId) {
        return query(npcId).actor().flatMap(NpcActorView::restAttachment);
    }

    default NpcResults.QueryOperation operation(String operationId) {
        return new NpcResults.QueryOperation(NpcResults.OperationQueryStatus.NOT_FOUND,
                Optional.empty(), "Операция не найдена.");
    }

    default NpcResults.ListOperations listOperations(int limit) {
        return new NpcResults.ListOperations(java.util.List.of(), false);
    }

    default NpcResults.ListActors blockedActors(int limit) {
        return new NpcResults.ListActors(java.util.List.of(), false);
    }

    default NpcResults.Spawn retryRecovery(ActorId actorId) {
        return new NpcResults.Spawn(NpcResults.SpawnStatus.REJECTED, Optional.empty(),
                "Повтор recovery недоступен для этой реализации.");
    }

    private static NpcPlaceStatus placeStatus(NpcResults.TeleportStatus status) {
        return switch (status) {
            case QUEUED -> NpcPlaceStatus.QUEUED;
            case MOVED -> NpcPlaceStatus.PLACED;
            case NOT_FOUND -> NpcPlaceStatus.NOT_FOUND;
            case WORLD_NOT_FOUND, INVALID_REQUEST -> NpcPlaceStatus.TARGET_INVALID;
            case SERVICE_STOPPED -> NpcPlaceStatus.SERVICE_STOPPED;
            case FAILURE -> NpcPlaceStatus.FAILURE;
            case REJECTED, PLATFORM_REJECTED -> NpcPlaceStatus.REJECTED;
        };
    }
}
