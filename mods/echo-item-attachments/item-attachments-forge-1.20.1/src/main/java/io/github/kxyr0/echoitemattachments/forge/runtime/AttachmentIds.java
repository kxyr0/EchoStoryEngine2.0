package io.github.kxyr0.echoitemattachments.forge.runtime;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import java.util.UUID;

final class AttachmentIds {
    private AttachmentIds() {
    }

    static AttachmentId attachment() {
        return new AttachmentId("echo_item_attachments:" + UUID.randomUUID());
    }

    static CustodyId custody() {
        return new CustodyId("echo_item_attachments:" + UUID.randomUUID());
    }

    static ReleaseId release() {
        return new ReleaseId("echo_item_attachments:" + UUID.randomUUID());
    }
}
