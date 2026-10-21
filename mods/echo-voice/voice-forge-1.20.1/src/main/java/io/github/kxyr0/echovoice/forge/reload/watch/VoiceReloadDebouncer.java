package io.github.kxyr0.echovoice.forge.reload.watch;

import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Coalesces create/modify/delete/rename events by extension-independent asset key. */
public final class VoiceReloadDebouncer {
    private final Map<VoiceAssetKey, Long> touchedAt = new LinkedHashMap<>();

    public synchronized void touch(VoiceAssetKey key, long nowMillis) {
        touchedAt.put(key, nowMillis);
    }

    public synchronized List<VoiceAssetKey> due(long nowMillis, long debounceMillis) {
        List<VoiceAssetKey> due = new ArrayList<>();
        touchedAt.entrySet().removeIf(entry -> {
            if (nowMillis - entry.getValue() >= debounceMillis) {
                due.add(entry.getKey());
                return true;
            }
            return false;
        });
        return List.copyOf(due);
    }
}
