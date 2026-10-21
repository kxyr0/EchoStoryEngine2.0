package io.github.kxyr0.echonpc.core.actor;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.spatial.BlockPosition;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.core.definition.NpcDefinition;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Server-scoped durable desired actor registry. It never treats platform handles as logical identity.
 */
public final class ActorRuntime {
    private final DiagnosticSink diagnostics;
    private final Map<ActorId, ActiveActor> actors = new HashMap<>();
    private boolean active;

    public ActorRuntime(DiagnosticSink diagnostics) {
        this.diagnostics = Objects.requireNonNull(diagnostics, "diagnostics");
    }

    public void start() {
        active = true;
    }

    public void stop() {
        actors.clear();
        active = false;
    }

    public boolean active() {
        return active;
    }

    public NpcResults.Query query(ActorId actorId) {
        if (!active) {
            return new NpcResults.Query(NpcResults.QueryStatus.SERVICE_STOPPED, Optional.empty(), "Сервис остановлен.");
        }
        ActiveActor actor = actors.get(actorId);
        if (actor == null) {
            return new NpcResults.Query(NpcResults.QueryStatus.NOT_FOUND, Optional.empty(), "Актёр не найден.");
        }
        return new NpcResults.Query(NpcResults.QueryStatus.FOUND, Optional.of(actor.view()), "Актёр найден.");
    }

    public NpcResults.ListActors listActors(int limit) {
        int bounded = Math.max(0, Math.min(limit, 200));
        var views = actors.values().stream()
                .map(ActiveActor::view)
                .sorted(Comparator.comparing(NpcActorView::actorId))
                .limit(bounded)
                .toList();
        return new NpcResults.ListActors(views, actors.size() > bounded);
    }

    public NpcResults.ListActors blockedActors(int limit) {
        int bounded = Math.max(0, Math.min(limit, 200));
        List<NpcActorView> views = actors.values().stream()
                .filter(actor -> actor.view().blockedReason().isPresent())
                .map(ActiveActor::view)
                .sorted(Comparator.comparing(NpcActorView::actorId))
                .limit(bounded)
                .toList();
        return new NpcResults.ListActors(views, blockedCount() > bounded);
    }

    public Optional<ActiveActor> actor(ActorId actorId) {
        return Optional.ofNullable(actors.get(actorId));
    }

    public void install(ActorDesiredState desired, Optional<NpcDefinition> definition,
            NpcActorView.Status status, Optional<String> blockedReason) {
        if (desired.presence() == ActorPresence.ABSENT) {
            actors.remove(desired.actorId());
            return;
        }
        actors.put(desired.actorId(), new ActiveActor(desired, definition, Optional.empty(), status,
                Optional.empty(), blockedReason));
    }

    public void updateDesired(ActorDesiredState desired, Optional<NpcDefinition> definition,
            NpcActorView.Status status, Optional<String> reason) {
        ActiveActor current = actors.get(desired.actorId());
        if (current == null) {
            install(desired, definition, status, reason);
            return;
        }
        actors.put(desired.actorId(), current.desired(desired, definition, status, reason));
    }

    public void markMaterialized(ActorId actorId, io.github.kxyr0.echonpc.core.platform.PlatformActorHandle handle) {
        ActiveActor actor = actors.get(actorId);
        if (actor != null) {
            actors.put(actorId, actor.materialized(handle));
        }
    }

    public void markDetached(ActorId actorId, NpcActorView.Status status, Optional<String> reason) {
        ActiveActor actor = actors.get(actorId);
        if (actor != null) {
            actors.put(actorId, actor.detached(status, reason));
        }
    }

    public void remove(ActorId actorId) {
        actors.remove(actorId);
    }

    public int activeActorCount() {
        return (int) actors.values().stream()
                .filter(actor -> actor.desired().presence() == ActorPresence.ACTIVE)
                .count();
    }

    public int durableActorCount() {
        return actors.size();
    }

    public int blockedCount() {
        return (int) actors.values().stream()
                .filter(actor -> actor.view().blockedReason().isPresent())
                .count();
    }

    public boolean restOccupied(BlockPosition canonicalBed, ActorId exceptActorId) {
        return actors.values().stream()
                .filter(actor -> !actor.desired().actorId().equals(exceptActorId))
                .flatMap(actor -> actor.desired().restAttachment().stream())
                .anyMatch(rest -> rest.occupied() && rest.canonicalBedPosition().equals(canonicalBed));
    }

    public void reportMissing(ActorId actorId) {
        diagnostics.accept(NpcDiagnostic.warning("ENPC-ACTOR-ENTITY-MISSING", "Платформенная entity исчезла.",
                actorId.value()));
    }
}
