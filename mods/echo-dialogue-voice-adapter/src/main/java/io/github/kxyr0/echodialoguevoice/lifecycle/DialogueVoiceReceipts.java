package io.github.kxyr0.echodialoguevoice.lifecycle;

import io.github.kxyr0.echodialogue.api.identity.DialogueSessionId;
import io.github.kxyr0.echovoice.api.identity.VoiceAssetKey;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Adapter-owned bounded replay memory; it persists across client disconnect events. */
final class DialogueVoiceReceipts {
    private final int limit;
    private final Map<Key, Status> values = new LinkedHashMap<>(16, 0.75F, true);

    DialogueVoiceReceipts(int limit) {
        this.limit = limit;
    }

    synchronized void record(Key key, Status status) {
        values.put(key, status);
        while (values.size() > limit) {
            values.remove(values.keySet().iterator().next());
        }
    }

    synchronized Optional<Status> find(Key key) {
        return Optional.ofNullable(values.get(key));
    }

    record Key(DialogueSessionId sessionId, int revision, VoiceAssetKey assetKey) {
    }

    enum Status {
        PLAYING,
        COMPLETED,
        INTERRUPTED
    }
}
