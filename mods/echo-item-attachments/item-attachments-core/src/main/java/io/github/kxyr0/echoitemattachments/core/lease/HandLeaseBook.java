package io.github.kxyr0.echoitemattachments.core.lease;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.core.custody.CustodyConflictException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class HandLeaseBook {
    private final Map<Key, HandLease> leases = new LinkedHashMap<>();

    public synchronized HandLease acquire(AttachmentSubjectRef subject, AttachmentPointId point,
            AttachmentCommandId commandId, String ownerId) {
        Key key = new Key(subject, point);
        HandLease requested = new HandLease(subject, point, commandId, ownerId);
        HandLease current = leases.get(key);
        if (current == null) {
            leases.put(key, requested);
            return requested;
        }
        if (current.commandId().equals(commandId) && current.ownerId().equals(ownerId)) return current;
        throw new CustodyConflictException("EIA-HAND-LEASE-CONFLICT",
                "Hand is leased by command " + current.commandId().value());
    }

    public synchronized void release(AttachmentSubjectRef subject, AttachmentPointId point,
            AttachmentCommandId commandId) {
        Key key = new Key(subject, point);
        HandLease current = leases.get(key);
        if (current != null && current.commandId().equals(commandId)) leases.remove(key);
    }

    public synchronized void releaseCommand(AttachmentCommandId commandId) {
        leases.entrySet().removeIf(entry -> entry.getValue().commandId().equals(commandId));
    }

    public synchronized Optional<HandLease> active(
            AttachmentSubjectRef subject, AttachmentPointId point) {
        return Optional.ofNullable(leases.get(new Key(subject, point)));
    }

    public synchronized void clear() {
        leases.clear();
    }

    private record Key(AttachmentSubjectRef subject, AttachmentPointId point) {
    }
}
