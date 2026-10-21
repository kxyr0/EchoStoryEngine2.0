package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.query.AnimationPlaybackQueryResult;
import io.github.kxyr0.echoanimation.forge.api.AnimationServiceBridge;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentServiceBridge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.server.ServerLifecycleHooks;

final class AttachmentAnimationMonitor {
    private final ActivePlaybackBindings bindings;
    private final AnimationSubjectMapping subjects = new AnimationSubjectMapping();

    AttachmentAnimationMonitor(ActivePlaybackBindings bindings) {
        this.bindings = bindings;
    }

    void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) return;
        var animations = AnimationServiceBridge.lookup(server);
        var attachments = ItemAttachmentServiceBridge.lookup(server);
        if (animations.status() != io.github.kxyr0.echoanimation.forge.api.AnimationServiceLookupResult.Status.RESOLVED
                || attachments.status() != ItemAttachmentServiceBridge.Status.RESOLVED) return;
        bindings.snapshot().forEach((playbackId, binding) -> {
            var query = animations.service().orElseThrow().queryAnimation(
                    subjects.animation(binding.subject()), new AnimationId(binding.animationId()));
            if (active(query, playbackId) || transientFailure(query.status())) return;
            attachments.service().orElseThrow().cancel(binding.commandId());
            bindings.remove(playbackId);
        });
    }

    private boolean active(AnimationPlaybackQueryResult query, String playbackId) {
        return query.status() == AnimationPlaybackQueryResult.Status.ACTIVE
                && query.playbackId().map(value -> value.value().equals(playbackId)).orElse(false);
    }

    private boolean transientFailure(AnimationPlaybackQueryResult.Status status) {
        return status == AnimationPlaybackQueryResult.Status.DEFINITION_UNAVAILABLE
                || status == AnimationPlaybackQueryResult.Status.SUBJECT_NOT_FOUND;
    }
}
