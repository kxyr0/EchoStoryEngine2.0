package io.github.kxyr0.echoanimationeditor.api.session;

public record AnimatorDraftRevision(long value) implements Comparable<AnimatorDraftRevision> {
    public AnimatorDraftRevision next() {
        return new AnimatorDraftRevision(value + 1L);
    }

    @Override
    public int compareTo(AnimatorDraftRevision other) {
        return Long.compare(value, other.value);
    }
}
