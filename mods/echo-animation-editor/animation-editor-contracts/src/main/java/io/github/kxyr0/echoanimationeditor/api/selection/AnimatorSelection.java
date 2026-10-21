package io.github.kxyr0.echoanimationeditor.api.selection;

import java.util.Objects;
import java.util.Optional;

public record AnimatorSelection(
        Optional<String> subjectId,
        Optional<String> clipId,
        Optional<String> boneId,
        Optional<String> faceElementId
) {
    public AnimatorSelection {
        subjectId = Objects.requireNonNull(subjectId, "subjectId");
        clipId = Objects.requireNonNull(clipId, "clipId");
        boneId = Objects.requireNonNull(boneId, "boneId");
        faceElementId = Objects.requireNonNull(faceElementId, "faceElementId");
    }

    public static AnimatorSelection empty() {
        return new AnimatorSelection(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
    }

    public AnimatorSelection bone(String nextBoneId) {
        return new AnimatorSelection(subjectId, clipId, Optional.of(nextBoneId), faceElementId);
    }

    public AnimatorSelection faceElement(String nextFaceElementId) {
        return new AnimatorSelection(subjectId, clipId, boneId, Optional.of(nextFaceElementId));
    }
}
