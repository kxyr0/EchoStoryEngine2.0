package io.github.kxyr0.echoteleportmagic.api;

import java.util.UUID;

/** Immutable inspection: execution identity is distinct from the pinned profile identity. */
public record TeleportExecution(TeleportExecutionId id, UUID subject, TeleportAnchor source,
        TeleportDestination destination, long startTick, long seed, TeleportState state,
        boolean committed, boolean preview, String owner, String provenance, int profileRevision, String rootOwner, String subjectReference) {
    public TeleportExecution(TeleportExecutionId id, UUID subject, TeleportAnchor source,
            TeleportDestination destination, long startTick, long seed, TeleportState state,
            boolean committed, boolean preview, String owner, String provenance, int profileRevision) {
        this(id, subject, source, destination, startTick, seed, state, committed, preview, owner, provenance, profileRevision, owner, subject.toString());
    }
    public TeleportExecution(TeleportExecutionId id, UUID subject, TeleportAnchor source,
            TeleportDestination destination, long startTick, long seed, TeleportState state,
            boolean committed, boolean preview, String owner, String provenance, int profileRevision, String rootOwner) {
        this(id, subject, source, destination, startTick, seed, state, committed, preview, owner, provenance, profileRevision, rootOwner, subject.toString());
    }
    public TeleportExecution state(TeleportState next, boolean didCommit) {
        return new TeleportExecution(id, subject, source, destination, startTick, seed, next,
                didCommit, preview, owner, provenance, profileRevision, rootOwner, subjectReference);
    }
    public TeleportExecution anchor(TeleportAnchor anchor) {
        return new TeleportExecution(id, subject, anchor, destination, startTick, seed, state,
                committed, preview, owner, provenance, profileRevision, rootOwner, subjectReference);
    }
}
