package io.github.kxyr0.echocutscene.api.runtime;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutscenePlaybackId;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record CutsceneResult(
        CutsceneOutcome outcome,
        Optional<CutscenePlaybackId> playbackId,
        List<CutsceneDiagnostic> diagnostics
) {
    public CutsceneResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        playbackId = Objects.requireNonNull(playbackId, "playbackId");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public static CutsceneResult accepted(CutscenePlaybackId playbackId) {
        return new CutsceneResult(CutsceneOutcome.ACCEPTED, Optional.of(playbackId), List.of());
    }

    public static CutsceneResult failed(String code, String message) {
        return new CutsceneResult(CutsceneOutcome.FAILED, Optional.empty(),
                List.of(CutsceneDiagnostic.of(code, message)));
    }
}
