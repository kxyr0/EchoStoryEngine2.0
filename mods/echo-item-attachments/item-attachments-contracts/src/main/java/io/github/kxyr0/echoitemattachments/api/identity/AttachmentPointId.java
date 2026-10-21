package io.github.kxyr0.echoitemattachments.api.identity;

import java.util.Objects;

public record AttachmentPointId(String value) implements Comparable<AttachmentPointId> {
    public static final AttachmentPointId LEFT_HAND =
            new AttachmentPointId("echo_item_attachments:left_hand");
    public static final AttachmentPointId RIGHT_HAND =
            new AttachmentPointId("echo_item_attachments:right_hand");

    public AttachmentPointId {
        value = Objects.requireNonNull(value, "attachment point").trim();
        if (!value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("attachment point must be namespaced: " + value);
        }
    }

    public boolean isHand() {
        return equals(LEFT_HAND) || equals(RIGHT_HAND);
    }

    @Override
    public int compareTo(AttachmentPointId other) {
        return value.compareTo(other.value);
    }
}
