package io.github.kxyr0.echotrigger.core.reload;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TriggerReloadPlanner {
    private final Map<Path, TriggerResourceFingerprint> known = new LinkedHashMap<>();

    public TriggerReloadPlan plan(Map<Path, String> currentSources) {
        List<Path> changed = currentSources.entrySet().stream()
                .filter(entry -> changed(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .toList();
        changed.forEach(path -> known.put(path, TriggerResourceFingerprint.sha256(currentSources.get(path))));
        return new TriggerReloadPlan(changed, changed.size(), changed.size());
    }

    private boolean changed(Path path, String source) {
        TriggerResourceFingerprint fingerprint = TriggerResourceFingerprint.sha256(source);
        return !fingerprint.equals(known.get(path));
    }
}
