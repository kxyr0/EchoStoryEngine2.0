package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoitemattachments.forge.api.animation.AttachmentAnimationStartEvent;
import io.github.kxyr0.echoitemattachments.forge.api.service.PlaybackBinding;
import java.nio.charset.StandardCharsets;

final class AttachmentAnimationStarter {
    private final ActivePlaybackBindings bindings;
    private final AnimationSubjectMapping subjects = new AnimationSubjectMapping();

    AttachmentAnimationStarter(ActivePlaybackBindings bindings) {
        this.bindings = bindings;
    }

    void start(AttachmentAnimationStartEvent event) {
        var lookup = AnimationServiceBridge.lookup(event.server());
        if (lookup.status() != io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult.Status.RESOLVED) {
            return;
        }
        var service = lookup.service().orElseThrow();
        AnimationId animation = new AnimationId(event.animationId());
        String markerId = event.markerId().contains(":") ? event.markerId()
                : animation.value().substring(0, animation.value().indexOf(':') + 1) + event.markerId();
        var marker = service.animationMarkers(animation).stream()
                .filter(value -> value.id().value().equals(markerId)).toList();
        if (!service.animations().contains(animation) || marker.size() != 1) return;
        var subject = subjects.animation(event.subject());
        if (!service.ensureSubject(subject)) return;
        java.util.UUID commandUuid = java.util.UUID.nameUUIDFromBytes(event.commandId().value()
                .getBytes(StandardCharsets.UTF_8));
        var result = service.playAnimation(animation, subject,
                AnimationCommandId.parse("echo_item_attachments:" + commandUuid));
        if (result.status() != AnimationPlayResult.Status.STARTED
                && result.status() != AnimationPlayResult.Status.ALREADY_APPLIED) return;
        var playback = result.playbackId().orElse(null);
        if (playback == null) return;
        var query = service.queryAnimation(subject, animation);
        if (query.playbackId().filter(playback::equals).isEmpty() || query.playbackRevision() < 1L) return;
        String phase = marker.get(0).phase().name();
        PlaybackBinding binding = new PlaybackBinding(event.commandId(), animation.value(),
                playback.value(), query.playbackRevision(), phase, markerId);
        bindings.put(playback.value(), new ActivePlaybackBindings.Binding(event.commandId(),
                event.subject(), animation.value(), markerId));
        event.bind(binding);
    }
}
