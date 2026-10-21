package io.github.kxyr0.echocutscene.core.lifecycle;

import io.github.kxyr0.echocutscene.api.identity.cutscene.CutsceneCommandId;
import io.github.kxyr0.echocutscene.api.runtime.CutsceneResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class CommandIdempotencyLedger {
    private final Map<CutsceneCommandId, CutsceneResult> results = new HashMap<>();

    public Optional<CutsceneResult> previous(CutsceneCommandId commandId) {
        return Optional.ofNullable(results.get(commandId));
    }

    public CutsceneResult remember(CutsceneCommandId commandId, CutsceneResult result) {
        results.put(commandId, result);
        return result;
    }
}
