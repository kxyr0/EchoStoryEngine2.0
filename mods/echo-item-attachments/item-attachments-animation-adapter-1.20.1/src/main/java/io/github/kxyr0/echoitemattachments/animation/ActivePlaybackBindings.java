package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoitemattachments.api.identity.AttachmentCommandId;
import io.github.kxyr0.echoitemattachments.api.identity.AttachmentSubjectRef;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

final class ActivePlaybackBindings {
    private final Map<String, Binding> byPlayback = new LinkedHashMap<>();

    synchronized void put(String playbackId, Binding binding) {
        Binding prior = byPlayback.putIfAbsent(playbackId, binding);
        if (prior != null && !prior.equals(binding)) {
            throw new IllegalStateException("playback is already bound to another item command");
        }
        while (byPlayback.size() > 2048) byPlayback.remove(byPlayback.keySet().iterator().next());
    }

    synchronized Optional<Binding> find(String playbackId) {
        return Optional.ofNullable(byPlayback.get(playbackId));
    }

    synchronized void remove(String playbackId) {
        byPlayback.remove(playbackId);
    }

    synchronized Map<String, Binding> snapshot() {
        return Map.copyOf(byPlayback);
    }

    synchronized Optional<String> playback(AttachmentSubjectRef subject) {
        var values = byPlayback.entrySet().stream()
                .filter(entry -> entry.getValue().subject().equals(subject)).map(Map.Entry::getKey).toList();
        return values.size() == 1 ? Optional.of(values.get(0)) : Optional.empty();
    }

    record Binding(AttachmentCommandId commandId, AttachmentSubjectRef subject,
            String animationId, String markerId) {
    }
}
