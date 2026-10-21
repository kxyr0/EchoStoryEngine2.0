package io.github.kxyr0.echocutscene.api.definition;

import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackId;
import io.github.kxyr0.echocutscene.api.identity.camera.CameraTrackRevision;
import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneEndBehavior;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneHudPolicy;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneInputPolicy;
import java.util.Objects;

public record CutsceneDefinition(
        CutsceneId cutsceneId,
        CameraTrackId cameraTrackId,
        CameraTrackRevision pinnedRevision,
        double speed,
        CutsceneInputPolicy inputPolicy,
        CutsceneHudPolicy hudPolicy,
        CutsceneEndBehavior endBehavior
) {
    public CutsceneDefinition(
            CutsceneId cutsceneId,
            CameraTrackId cameraTrackId,
            CameraTrackRevision pinnedRevision,
            double speed,
            CutsceneInputPolicy inputPolicy,
            CutsceneHudPolicy hudPolicy
    ) {
        this(cutsceneId, cameraTrackId, pinnedRevision, speed, inputPolicy, hudPolicy,
                CutsceneEndBehavior.AUTO_STOP);
    }

    public CutsceneDefinition {
        cutsceneId = Objects.requireNonNull(cutsceneId, "cutsceneId");
        cameraTrackId = Objects.requireNonNull(cameraTrackId, "cameraTrackId");
        pinnedRevision = Objects.requireNonNull(pinnedRevision, "pinnedRevision");
        inputPolicy = Objects.requireNonNull(inputPolicy, "inputPolicy");
        hudPolicy = Objects.requireNonNull(hudPolicy, "hudPolicy");
        endBehavior = Objects.requireNonNull(endBehavior, "endBehavior");
        if (!Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("speed must be finite and positive.");
        }
    }

    public CutsceneDefinition withEndBehavior(CutsceneEndBehavior nextEndBehavior) {
        return new CutsceneDefinition(cutsceneId, cameraTrackId, pinnedRevision, speed, inputPolicy,
                hudPolicy, nextEndBehavior);
    }
}
