package io.github.kxyr0.echonpcbackpack.forge.runtime;

import io.github.kxyr0.echonpcbackpack.api.payload.NpcBackpackPayloadRef;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackStatus;
import io.github.kxyr0.echonpcbackpack.api.transition.NpcBackpackTransitionSnapshot;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackDiagnostic;
import io.github.kxyr0.echonpcbackpack.api.view.NpcBackpackView;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.BackpackFingerprint;
import io.github.kxyr0.echonpcbackpack.forge.sophisticated.SophisticatedBackpackAccess;
import io.github.kxyr0.echonpcbackpack.forge.state.BackpackActorRecord;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;

public final class BackpackViewMapper {
    private final SophisticatedBackpackAccess access;
    private final BackpackFingerprint fingerprints = new BackpackFingerprint();

    public BackpackViewMapper(SophisticatedBackpackAccess access) {
        this.access = access;
    }

    public NpcBackpackView view(BackpackActorRecord record) {
        Optional<NpcBackpackPayloadRef> payload = access.contentsUuid(record.payload).map(uuid ->
                new NpcBackpackPayloadRef(uuid,
                        BuiltInRegistries.ITEM.getKey(record.payload.getItem()).toString(),
                        record.expectedFingerprint.orElseGet(() -> fingerprints.exact(record.payload, uuid))));
        Optional<NpcBackpackTransitionSnapshot> transition = record.transitionId.map(id ->
                new NpcBackpackTransitionSnapshot(id, record.transitionType.orElseThrow(),
                        record.phase.orElseThrow(), record.policy, record.source, record.destination,
                        record.revision));
        Optional<NpcBackpackDiagnostic> diagnostic = "ENPC-SB-OK".equals(record.diagnosticCode)
                ? Optional.empty() : Optional.of(new NpcBackpackDiagnostic(severity(record.status),
                record.diagnosticCode, record.diagnosticMessage));
        return new NpcBackpackView(record.actorId, record.status, record.attachmentPoint,
                payload, transition, diagnostic, record.revision);
    }

    private NpcBackpackDiagnostic.Severity severity(NpcBackpackStatus status) {
        return status == NpcBackpackStatus.RECOVERY_REQUIRED
                ? NpcBackpackDiagnostic.Severity.ERROR : NpcBackpackDiagnostic.Severity.WARNING;
    }
}
