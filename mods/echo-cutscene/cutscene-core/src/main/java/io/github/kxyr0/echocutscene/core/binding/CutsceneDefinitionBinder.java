package io.github.kxyr0.echocutscene.core.binding;

import io.github.kxyr0.echocutscene.api.camera.CameraTrackSnapshot;
import io.github.kxyr0.echocutscene.api.definition.CutsceneDefinition;
import java.util.List;
import java.util.Objects;

public final class CutsceneDefinitionBinder {
    public List<BindingDiagnostic> validate(CutsceneDefinition definition, CameraTrackSnapshot track) {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(track, "track");
        if (!definition.cameraTrackId().equals(track.metadata().trackId())) {
            return List.of(new BindingDiagnostic("CUTSCENE_TRACK_NOT_FOUND",
                    "Cutscene references a different camera track."));
        }
        if (!definition.pinnedRevision().equals(track.metadata().revision())) {
            return List.of(new BindingDiagnostic("CUTSCENE_TRACK_REVISION_MISMATCH",
                    "Cutscene references a different camera track revision."));
        }
        return List.of();
    }
}
