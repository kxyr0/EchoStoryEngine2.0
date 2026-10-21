package io.github.kxyr0.echoitemattachments.core.custody;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import io.github.kxyr0.echoitemattachments.core.model.CustodyRecord;
import io.github.kxyr0.echoitemattachments.core.model.ReleasePlan;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class AttachmentLedger {
    private final Map<AttachmentId, CustodyRecord> records = new LinkedHashMap<>();

    public synchronized CustodyRecord reserveWorld(AttachmentId attachmentId, CustodyId custodyId,
            AttachmentSubjectRef subject, AttachmentPointId point, String itemId, int count,
            int sourceCountBefore, byte[] stackTag, String sourceEntityId) {
        if (records.containsKey(attachmentId)) {
            throw conflict("EIA-CUSTODY-ID-CONFLICT", "Attachment id already exists.");
        }
        requirePointFree(subject, point, null);
        CustodyRecord record = new CustodyRecord(attachmentId, custodyId, subject, point, itemId,
                count, sourceCountBefore, stackTag, ItemCustodyState.WORLD_ITEM, 1L,
                Optional.of(sourceEntityId), Optional.empty(), Optional.empty());
        records.put(attachmentId, record);
        return record;
    }

    public synchronized CustodyRecord attach(AttachmentId id) {
        CustodyRecord current = require(id, ItemCustodyState.WORLD_ITEM);
        requirePointFree(current.subject(), current.point(), id);
        return replace(current.advance(ItemCustodyState.ATTACHED_TO_SUBJECT, current.point(),
                Optional.empty(), Optional.empty()));
    }

    public synchronized CustodyRecord beginAttach(AttachmentId id) {
        CustodyRecord current = require(id, ItemCustodyState.WORLD_ITEM);
        return replace(current.advance(ItemCustodyState.RECOVERING, current.point(),
                current.sourceEntityId(), Optional.empty()));
    }

    public synchronized CustodyRecord completeAttach(AttachmentId id) {
        CustodyRecord current = require(id, ItemCustodyState.RECOVERING);
        requirePointFree(current.subject(), current.point(), id);
        return replace(current.advance(ItemCustodyState.ATTACHED_TO_SUBJECT, current.point(),
                Optional.empty(), Optional.empty()));
    }

    public synchronized CustodyRecord move(AttachmentId id, AttachmentPointId target) {
        CustodyRecord current = require(id, ItemCustodyState.ATTACHED_TO_SUBJECT);
        requirePointFree(current.subject(), target, id);
        return replace(current.advance(ItemCustodyState.ATTACHED_TO_SUBJECT, target,
                Optional.empty(), Optional.empty()));
    }

    public synchronized CustodyRecord beginRelease(
            AttachmentId id, ReleaseId releaseId, ReleasePlan plan) {
        CustodyRecord current = require(id, ItemCustodyState.ATTACHED_TO_SUBJECT);
        return replace(current.startRelease(releaseId, plan));
    }

    public synchronized CustodyRecord confirmReleased(AttachmentId id) {
        CustodyRecord current = require(id, ItemCustodyState.RELEASING);
        return replace(current.advance(ItemCustodyState.WORLD_RELEASED, current.point(),
                Optional.empty(), current.releaseId()));
    }

    public synchronized void restore(CustodyRecord record) {
        CustodyRecord prior = records.putIfAbsent(record.attachmentId(), record);
        if (prior != null && !prior.equals(record)) {
            throw conflict("EIA-CUSTODY-RESTORE-CONFLICT", "Conflicting restored custody record.");
        }
        if (occupies(record.state())) requirePointFree(record.subject(), record.point(), record.attachmentId());
    }

    public synchronized Optional<CustodyRecord> find(AttachmentId id) {
        return Optional.ofNullable(records.get(id));
    }

    public synchronized Optional<CustodyRecord> attached(
            AttachmentSubjectRef subject, AttachmentPointId point) {
        return records.values().stream().filter(record -> record.subject().equals(subject)
                && record.point().equals(point) && occupies(record.state())).findFirst();
    }

    public synchronized Collection<CustodyRecord> records() {
        return new ArrayList<>(records.values());
    }

    public synchronized void forgetReleased(AttachmentId id) {
        CustodyRecord record = require(id, ItemCustodyState.WORLD_RELEASED);
        records.remove(record.attachmentId());
    }

    public synchronized void abandonWorld(AttachmentId id) {
        CustodyRecord record = require(id, ItemCustodyState.WORLD_ITEM);
        records.remove(record.attachmentId());
    }

    public synchronized void abandonRecovery(AttachmentId id) {
        CustodyRecord record = require(id, ItemCustodyState.RECOVERING);
        records.remove(record.attachmentId());
    }

    private CustodyRecord require(AttachmentId id, ItemCustodyState expected) {
        CustodyRecord record = records.get(id);
        if (record == null) throw conflict("EIA-CUSTODY-NOT-FOUND", "Custody record does not exist.");
        if (record.state() != expected) {
            throw conflict("EIA-CUSTODY-STATE", "Expected " + expected + " but was " + record.state());
        }
        return record;
    }

    private void requirePointFree(AttachmentSubjectRef subject, AttachmentPointId point,
            AttachmentId ignored) {
        boolean occupied = records.values().stream().anyMatch(record -> !record.attachmentId().equals(ignored)
                && record.subject().equals(subject) && record.point().equals(point) && occupies(record.state()));
        if (occupied) throw conflict("EIA-HAND-OCCUPIED", "Attachment point is already occupied.");
    }

    private boolean occupies(ItemCustodyState state) {
        return state == ItemCustodyState.WORLD_ITEM || state == ItemCustodyState.ATTACHED_TO_SUBJECT
                || state == ItemCustodyState.RELEASING || state == ItemCustodyState.RECOVERING;
    }

    private CustodyRecord replace(CustodyRecord record) {
        records.put(record.attachmentId(), record);
        return record;
    }

    private CustodyConflictException conflict(String code, String message) {
        return new CustodyConflictException(code, message);
    }
}
