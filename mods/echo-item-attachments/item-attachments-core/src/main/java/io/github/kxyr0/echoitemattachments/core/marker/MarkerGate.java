package io.github.kxyr0.echoitemattachments.core.marker;

import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Optional;

public final class MarkerGate {
    private final Map<AttachmentCommandId, MarkerBinding> active = new LinkedHashMap<>();
    private final Set<String> consumed = new HashSet<>();

    public synchronized void bind(MarkerBinding binding) {
        MarkerBinding prior = active.putIfAbsent(binding.commandId(), binding);
        if (prior != null && !prior.equals(binding)) {
            throw new IllegalStateException("command already has a different marker binding");
        }
    }

    public synchronized MarkerDecision accept(AttachmentCommandId commandId, MarkerSignal signal) {
        MarkerBinding binding = active.get(commandId);
        if (binding == null) return decision(MarkerDecision.Status.NO_ACTIVE_BINDING, "EIA-MARKER-NO-BINDING");
        if (signal.playbackRevision() < binding.playbackRevision()) {
            return decision(MarkerDecision.Status.STALE, "EIA-MARKER-STALE");
        }
        if (!matches(binding, signal)) {
            return decision(MarkerDecision.Status.MISMATCH, "EIA-MARKER-MISMATCH");
        }
        String occurrence = signal.playbackId() + ":" + signal.playbackRevision() + ":"
                + signal.phase() + ":" + signal.markerId();
        if (!consumed.add(occurrence)) {
            return decision(MarkerDecision.Status.DUPLICATE, "EIA-MARKER-DUPLICATE");
        }
        active.remove(commandId);
        return decision(MarkerDecision.Status.ACCEPTED, "EIA-MARKER-ACCEPTED");
    }

    public synchronized void cancel(AttachmentCommandId commandId) {
        active.remove(commandId);
    }

    public synchronized boolean hasBinding(AttachmentCommandId commandId) {
        return active.containsKey(commandId);
    }

    public synchronized Optional<MarkerBinding> binding(AttachmentCommandId commandId) {
        return Optional.ofNullable(active.get(commandId));
    }

    private boolean matches(MarkerBinding binding, MarkerSignal signal) {
        return binding.subject().equals(signal.subject())
                && binding.animationId().equals(signal.animationId())
                && binding.playbackId().equals(signal.playbackId())
                && binding.playbackRevision() == signal.playbackRevision()
                && binding.phase().equals(signal.phase())
                && binding.markerId().equals(signal.markerId());
    }

    private MarkerDecision decision(MarkerDecision.Status status, String code) {
        return new MarkerDecision(status, code);
    }
}
