package io.github.kxyr0.echoanimationauthoring.api.draft;

import java.nio.file.Path;
import java.util.Objects;

public record AuthoringDraftView(
        AuthoringDraftId draftId,
        AuthoringDraftRevision revision,
        Path sourcePath,
        String clipId,
        String rigId,
        int keyframes,
        boolean dirty
) {
    public AuthoringDraftView {
        draftId = Objects.requireNonNull(draftId, "draftId");
        revision = Objects.requireNonNull(revision, "revision");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
        clipId = requireText(clipId, "clipId");
        rigId = requireText(rigId, "rigId");
        if (keyframes < 0) {
            throw new IllegalArgumentException("keyframes is negative");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " is blank");
        }
        return value;
    }
}
