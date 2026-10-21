package io.github.kxyr0.echonpc.forge.host;

import io.github.kxyr0.echonpc.api.identity.ActorId;
import io.github.kxyr0.echonpc.core.actor.ActorPresence;
import io.github.kxyr0.echonpc.core.actor.ActorRuntime;
import io.github.kxyr0.echonpc.core.actor.ActiveActor;
import io.github.kxyr0.echonpc.core.actor.EntityBindingId;
import io.github.kxyr0.echonpc.core.diagnostic.DiagnosticSink;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.core.platform.PlatformActorHandle;
import io.github.kxyr0.echonpc.forge.entity.EchoNpcEntity;
import io.github.kxyr0.echonpc.forge.recovery.ForgeNpcRecoveryFlow;
import java.util.Optional;

/**
 * Validates loaded entity NBT linkage against durable desired actor state.
 */
final class ForgeNpcEntityLinker {
    private final ActorRuntime runtime;
    private final ForgeActorMaterializer materializer;
    private final ForgeNpcRecoveryFlow recovery;
    private final DiagnosticSink diagnostics;

    ForgeNpcEntityLinker(ActorRuntime runtime, ForgeActorMaterializer materializer,
            ForgeNpcRecoveryFlow recovery, DiagnosticSink diagnostics) {
        this.runtime = java.util.Objects.requireNonNull(runtime, "runtime");
        this.materializer = java.util.Objects.requireNonNull(materializer, "materializer");
        this.recovery = java.util.Objects.requireNonNull(recovery, "recovery");
        this.diagnostics = java.util.Objects.requireNonNull(diagnostics, "diagnostics");
    }

    void reconcile(EchoNpcEntity entity) {
        ActorId actorId = parseActorId(entity.actorIdValue());
        EntityBindingId bindingId = parseBindingId(entity.bindingIdValue());
        Optional<ActiveActor> actor = runtime.actor(actorId);
        if (actor.isEmpty() || actor.orElseThrow().desired().presence() == ActorPresence.ABSENT) {
            diagnostics.accept(NpcDiagnostic.warning("ENPC-ENTITY-ORPHAN-REMOVED",
                    "Orphan Echo NPC entity удалена.", actorId.value()));
            entity.discard();
            return;
        }
        if (!actor.orElseThrow().desired().entityBindingId().equals(bindingId)) {
            diagnostics.accept(NpcDiagnostic.warning("ENPC-ENTITY-STALE-BINDING",
                    "Stale binding entity удалена.", actorId.value()));
            entity.discard();
            return;
        }
        recovery.recoverableDefinition(actor.orElseThrow().desired()).ifPresent(definition -> {
            Optional<PlatformActorHandle> existing = materializer.handle(actorId);
            if (existing.filter(handle -> !handle.value().equals(entity.getUUID().toString())).isPresent()) {
                diagnostics.accept(NpcDiagnostic.warning("ENPC-ENTITY-DUPLICATE-REMOVED",
                        "Duplicate loaded Echo NPC entity удалена.", actorId.value()));
                entity.discard();
                return;
            }
            PlatformActorHandle handle = existing.orElseGet(() -> materializer.adopt(entity,
                    actor.orElseThrow().desired(), definition,
                    recovery.activeForm(actor.orElseThrow().desired(), definition)));
            runtime.markMaterialized(actorId, handle);
        });
    }

    private ActorId parseActorId(String value) {
        try {
            return new ActorId(value);
        } catch (IllegalArgumentException exception) {
            return new ActorId("echo_npc:invalid_entity_actor");
        }
    }

    private EntityBindingId parseBindingId(String value) {
        try {
            return new EntityBindingId(value);
        } catch (IllegalArgumentException exception) {
            return EntityBindingId.generate();
        }
    }
}
