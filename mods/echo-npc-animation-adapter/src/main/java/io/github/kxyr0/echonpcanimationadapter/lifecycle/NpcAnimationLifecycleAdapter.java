package io.github.kxyr0.echonpcanimationadapter.lifecycle;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectLifecycleOwnerId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvenance;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningRequest;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvisioningResult;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.forge.api.AnimationService;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult;
import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.api.service.NpcActorView;
import io.github.kxyr0.echonpc.api.service.NpcResults;
import io.github.kxyr0.echonpc.api.service.NpcService;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceBridge;
import io.github.kxyr0.echonpc.forge.api.EchoNpcServiceLookupResult;
import io.github.kxyr0.echonpcanimationadapter.AnimationSubjectMapping;
import io.github.kxyr0.echonpcanimationadapter.NpcAnimationBindingDefinition;
import io.github.kxyr0.echonpcanimationadapter.resource.NpcAnimationBindingCatalogState;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;

/**
 * Server-thread reconciliation between durable NPC actors and animation subjects.
 */
public final class NpcAnimationLifecycleAdapter {
    public static final AnimationSubjectLifecycleOwnerId OWNER =
            AnimationSubjectLifecycleOwnerId.parse("echo_npc_animation:actor");
    private static final int SNAPSHOT_LIMIT = 200;
    private static final int RECORDS_PER_TICK = 64;

    private final NpcAnimationBindingCatalogState bindings;
    private final AnimationSubjectMapping mapping = new AnimationSubjectMapping();
    private Metrics lastMetrics = Metrics.empty("not-started");
    private String cursor = "";

    public NpcAnimationLifecycleAdapter(NpcAnimationBindingCatalogState bindings) {
        this.bindings = java.util.Objects.requireNonNull(bindings, "bindings");
    }

    public void tick(MinecraftServer server) {
        Optional<NpcService> npc = npcService(server);
        Optional<AnimationService> animation = animationService(server);
        if (npc.isEmpty() || animation.isEmpty()) {
            lastMetrics = Metrics.empty("service-unavailable");
            return;
        }
        reconcile(npc.orElseThrow(), animation.orElseThrow());
    }

    public AnimationSubjectProvisioningResult ensureActorSubject(NpcActorView actor, AnimationService service) {
        Optional<NpcAnimationBindingDefinition> binding = bindings.current()
                .resolve(actor.actorId(), actor.definitionId());
        if (binding.isPresent()) {
            return service.ensureSubject(new AnimationSubjectProvisioningRequest(
                    mapping.subjectId(actor.actorId()),
                    binding.orElseThrow().profileId(),
                    new AnimationSubjectProvenance(OWNER, actor.actorId().value()),
                    commandId("ensure", actor.actorId())));
        }

        /*
         * Locomotion must not depend on a hand-authored binding file. NPCs are
         * already a well-known humanoid integration, so create the legacy A3
         * subject when no explicit binding exists. Echo Animation then infers
         * the body channel and its idle source from the loaded animations.
         * Explicit bindings still win and keep full A4 profile/provenance.
         */
        var subjectId = mapping.subjectId(actor.actorId());
        AnimationProfileId implicitProfile = defaultProfile(actor);
        if (service.ensureSubject(subjectId)) {
            return new AnimationSubjectProvisioningResult(
                    AnimationSubjectProvisioningResult.Status.EXISTING,
                    subjectId,
                    implicitProfile,
                    Optional.empty(),
                    "EANIM-A4-IMPLICIT-HUMANOID",
                    "No explicit NPC animation binding; using implicit humanoid subject for "
                            + actor.actorId() + ".");
        }
        return unbound(actor);
    }

    private AnimationProfileId defaultProfile(NpcActorView actor) {
        String definition = actor.definitionId().value();
        int separator = definition.indexOf(':');
        String namespace = separator > 0 ? definition.substring(0, separator) : "jdh";
        return AnimationProfileId.parse(namespace + ":standard_humanoid");
    }

    public Metrics lastMetrics() {
        return lastMetrics;
    }

    private void reconcile(NpcService npc, AnimationService animation) {
        NpcResults.ListActors listed = npc.listActors(SNAPSHOT_LIMIT);
        List<NpcActorView> actors = listed.actors();
        List<NpcActorView> window = window(actors);
        int created = 0;
        int existing = 0;
        int pending = 0;
        int unbound = 0;
        int conflicts = 0;
        for (NpcActorView actor : window) {
            if (actor.desiredPresence() != NpcActorView.DesiredPresence.ACTIVE) {
                continue;
            }
            AnimationSubjectProvisioningResult result = ensureActorSubject(actor, animation);
            switch (result.status()) {
                case CREATED -> created++;
                case EXISTING -> existing++;
                case PROFILE_REBIND_REQUIRED -> pending++;
                case PROVENANCE_CONFLICT -> conflicts++;
                case PROFILE_NOT_FOUND -> unbound++;
                default -> {
                }
            }
        }
        int retired = listed.truncated() ? 0 : retireOrphans(actors, animation);
        lastMetrics = new Metrics(window.size(), created, existing, pending, unbound, conflicts, retired,
                listed.truncated(), "ok");
    }

    private List<NpcActorView> window(List<NpcActorView> actors) {
        if (actors.size() <= RECORDS_PER_TICK) {
            cursor = actors.isEmpty() ? "" : actors.get(actors.size() - 1).actorId().value();
            return actors;
        }
        List<NpcActorView> selected = actors.stream()
                .filter(actor -> actor.actorId().value().compareTo(cursor) > 0)
                .limit(RECORDS_PER_TICK)
                .toList();
        if (selected.isEmpty()) {
            selected = actors.stream().limit(RECORDS_PER_TICK).toList();
        }
        cursor = selected.get(selected.size() - 1).actorId().value();
        return selected;
    }

    private int retireOrphans(List<NpcActorView> actors, AnimationService animation) {
        Set<String> active = new HashSet<>();
        actors.stream()
                .filter(actor -> actor.desiredPresence() == NpcActorView.DesiredPresence.ACTIVE)
                .map(actor -> actor.actorId().value())
                .forEach(active::add);
        int retired = 0;
        for (var subjectId : animation.subjectsByOwner(OWNER)) {
            if (!active.contains(subjectId.value()) && animation.retireSubject(subjectId, OWNER)) {
                retired++;
                if (retired >= RECORDS_PER_TICK) {
                    break;
                }
            }
        }
        return retired;
    }

    private AnimationSubjectProvisioningResult unbound(NpcActorView actor) {
        return new AnimationSubjectProvisioningResult(
                AnimationSubjectProvisioningResult.Status.PROFILE_NOT_FOUND,
                mapping.subjectId(actor.actorId()),
                io.github.kxyr0.echoanimation.api.profile.AnimationProfileId.parse("echo_npc_animation:unbound"),
                Optional.of(new AnimationSubjectProvenance(OWNER, actor.actorId().value())),
                "EANIM-A4-UNBOUND",
                "No NPC animation binding for actor " + actor.actorId() + " definition " + actor.definitionId());
    }

    private Optional<NpcService> npcService(MinecraftServer server) {
        EchoNpcServiceLookupResult lookup = EchoNpcServiceBridge.lookup(server);
        return lookup.status() == EchoNpcServiceLookupResult.Status.RESOLVED ? lookup.service() : Optional.empty();
    }

    private Optional<AnimationService> animationService(MinecraftServer server) {
        AnimationServiceLookupResult lookup = AnimationServiceBridge.lookup(server);
        return lookup.status() == AnimationServiceLookupResult.Status.RESOLVED ? lookup.service() : Optional.empty();
    }

    private AnimationCommandId commandId(String action, ActorId actorId) {
        return AnimationCommandId.parse("echo_npc_animation:" + action + "/" + actorId.value().replace(':', '/'));
    }

    public record Metrics(
            int scanned,
            int created,
            int existing,
            int rebindPending,
            int unbound,
            int conflicts,
            int retired,
            boolean truncated,
            String status
    ) {
        public static Metrics empty(String status) {
            return new Metrics(0, 0, 0, 0, 0, 0, 0, false, status);
        }
    }
}
