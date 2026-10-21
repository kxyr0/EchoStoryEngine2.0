package io.github.kxyr0.echoanimationauthoring.api.draft;

public record AuthoringDraftRevision(long value) {
    public AuthoringDraftRevision next() {
        return new AuthoringDraftRevision(value + 1L);
    }
}
