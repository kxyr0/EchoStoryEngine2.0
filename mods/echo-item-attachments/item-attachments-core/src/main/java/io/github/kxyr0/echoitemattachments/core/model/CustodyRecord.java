package io.github.kxyr0.echoitemattachments.core.model;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentPointId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import io.github.kxyr0.echoitemattachments.api.identity.CustodyId;
import io.github.kxyr0.echoitemattachments.api.identity.ReleaseId;
import io.github.kxyr0.echoitemattachments.api.model.ItemCustodyState;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public record CustodyRecord(
        AttachmentId attachmentId,
        CustodyId custodyId,
        AttachmentSubjectRef subject,
        AttachmentPointId point,
        String itemId,
        int count,
        int sourceCountBefore,
        byte[] stackTag,
        ItemCustodyState state,
        long revision,
        Optional<String> sourceEntityId,
        Optional<ReleaseId> releaseId,
        Optional<ReleasePlan> releasePlan) {
    public CustodyRecord {
        attachmentId = Objects.requireNonNull(attachmentId, "attachment id");
        custodyId = Objects.requireNonNull(custodyId, "custody id");
        subject = Objects.requireNonNull(subject, "subject");
        point = Objects.requireNonNull(point, "point");
        itemId = Objects.requireNonNull(itemId, "item id");
        if (count < 1) throw new IllegalArgumentException("count must be positive");
        if (sourceCountBefore < count) throw new IllegalArgumentException("source count must cover payload");
        stackTag = Arrays.copyOf(Objects.requireNonNull(stackTag, "stack tag"), stackTag.length);
        if (stackTag.length == 0) throw new IllegalArgumentException("stack tag must be present");
        state = Objects.requireNonNull(state, "state");
        if (revision < 1) throw new IllegalArgumentException("revision must be positive");
        sourceEntityId = Objects.requireNonNull(sourceEntityId, "source entity id");
        releaseId = Objects.requireNonNull(releaseId, "release id");
        releasePlan = Objects.requireNonNull(releasePlan, "release plan");
    }

    @Override
    public byte[] stackTag() {
        return Arrays.copyOf(stackTag, stackTag.length);
    }

    public CustodyRecord advance(ItemCustodyState next, AttachmentPointId nextPoint,
            Optional<String> nextSource, Optional<ReleaseId> nextRelease) {
        CustodyTransitions.requireAllowed(state, next);
        return new CustodyRecord(attachmentId, custodyId, subject, nextPoint, itemId, count, sourceCountBefore,
                stackTag, next, revision + 1, nextSource, nextRelease, releasePlan);
    }

    public CustodyRecord startRelease(ReleaseId nextRelease, ReleasePlan plan) {
        CustodyTransitions.requireAllowed(state, ItemCustodyState.RELEASING);
        return new CustodyRecord(attachmentId, custodyId, subject, point, itemId, count, sourceCountBefore,
                stackTag, ItemCustodyState.RELEASING, revision + 1, Optional.empty(),
                Optional.of(nextRelease), Optional.of(plan));
    }
}
