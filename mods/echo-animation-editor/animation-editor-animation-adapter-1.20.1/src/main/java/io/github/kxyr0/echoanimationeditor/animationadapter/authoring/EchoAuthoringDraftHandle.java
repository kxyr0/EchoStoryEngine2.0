package io.github.kxyr0.echoanimationeditor.animationadapter.authoring;

import java.util.Objects;

public record EchoAuthoringDraftHandle(Object service, Object draftId, String clipId, String sourcePath) {
    public EchoAuthoringDraftHandle {
        service = Objects.requireNonNull(service, "service");
        draftId = Objects.requireNonNull(draftId, "draftId");
        clipId = Objects.requireNonNull(clipId, "clipId");
        sourcePath = Objects.requireNonNull(sourcePath, "sourcePath");
    }
}
