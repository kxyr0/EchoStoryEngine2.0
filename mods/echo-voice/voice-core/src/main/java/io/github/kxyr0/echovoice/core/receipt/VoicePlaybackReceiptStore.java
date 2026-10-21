package io.github.kxyr0.echovoice.core.receipt;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Bounded client receipt memory used by reconnect recovery decisions. */
public final class VoicePlaybackReceiptStore {
    private final int limit;
    private final Map<VoiceReceiptKey, VoiceReceiptStatus> receipts;

    public VoicePlaybackReceiptStore(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Receipt limit must be positive.");
        }
        this.limit = limit;
        this.receipts = new LinkedHashMap<>(16, 0.75F, true);
    }

    public synchronized void record(VoiceReceiptKey key, VoiceReceiptStatus status) {
        receipts.put(key, status);
        while (receipts.size() > limit) {
            receipts.remove(receipts.keySet().iterator().next());
        }
    }

    public synchronized Optional<VoiceReceiptStatus> find(VoiceReceiptKey key) {
        return Optional.ofNullable(receipts.get(key));
    }

    public synchronized boolean shouldReplay(VoiceReceiptKey key) {
        return find(key).map(status -> status == VoiceReceiptStatus.PLAYING
                || status == VoiceReceiptStatus.INTERRUPTED).orElse(true);
    }
}
