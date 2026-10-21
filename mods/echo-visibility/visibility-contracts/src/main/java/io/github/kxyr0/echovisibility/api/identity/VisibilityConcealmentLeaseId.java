package io.github.kxyr0.echovisibility.api.identity;

import java.util.Objects;

public record VisibilityConcealmentLeaseId(String value) implements Comparable<VisibilityConcealmentLeaseId> {
    public VisibilityConcealmentLeaseId {
        String checked = Objects.requireNonNull(value, "value").trim();
        if (checked.isEmpty() || checked.length() > 192) {
            throw new IllegalArgumentException("Concealment lease id must contain 1..192 characters.");
        }
        value = checked;
    }

    @Override
    public int compareTo(VisibilityConcealmentLeaseId other) {
        return value.compareTo(other.value);
    }
}
