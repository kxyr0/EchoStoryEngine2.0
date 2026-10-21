package io.github.kxyr0.echoitemattachments.forge.network;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachment;
import io.github.kxyr0.echoitemattachments.forge.api.client.ClientAttachmentLookupEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class ClientAttachmentStore {
    private static final Map<AttachmentSubjectRef, State> STATES = new LinkedHashMap<>();

    private ClientAttachmentStore() {
    }

    public static synchronized void accept(AttachmentSnapshotPayload payload) {
        State current = STATES.get(payload.subject());
        if (current != null && payload.revision() <= current.revision()) return;
        Map<AttachmentPointId, ClientAttachment> byPoint = new LinkedHashMap<>();
        payload.entries().forEach(entry -> byPoint.put(entry.point(), new ClientAttachment(
                entry.attachmentId(), entry.point(), entry.stack(), entry.revision())));
        STATES.put(payload.subject(), new State(payload.revision(), Map.copyOf(byPoint)));
    }

    public static synchronized void lookup(ClientAttachmentLookupEvent event) {
        find(event.subject(), event.point()).ifPresent(event::register);
    }

    public static synchronized Optional<ClientAttachment> find(
            AttachmentSubjectRef subject, AttachmentPointId point) {
        State state = STATES.get(subject);
        return state == null ? Optional.empty() : Optional.ofNullable(state.attachments().get(point));
    }

    public static synchronized void clear() {
        STATES.clear();
    }

    private record State(long revision, Map<AttachmentPointId, ClientAttachment> attachments) {
    }
}
