package io.github.kxyr0.echoitemattachments.core.command;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class CommandReplayGuard {
    private final Map<AttachmentCommandId, String> fingerprints = new LinkedHashMap<>();

    public synchronized boolean accepts(AttachmentCommandId id, String fingerprint) {
        String normalized = Objects.requireNonNull(fingerprint, "fingerprint");
        String prior = fingerprints.putIfAbsent(id, normalized);
        return prior == null || prior.equals(normalized);
    }
}
