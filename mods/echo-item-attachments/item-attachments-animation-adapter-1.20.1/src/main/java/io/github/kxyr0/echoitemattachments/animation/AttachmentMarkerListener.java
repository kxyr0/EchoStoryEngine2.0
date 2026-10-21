package io.github.kxyr0.echoitemattachments.animation;

import io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent;
import io.github.kxyr0.echoitemattachments.api.command.MarkerSignal;
import io.github.kxyr0.echoitemattachments.forge.api.service.ItemAttachmentServiceBridge;
import net.minecraftforge.server.ServerLifecycleHooks;

final class AttachmentMarkerListener {
    private final ActivePlaybackBindings bindings;

    AttachmentMarkerListener(ActivePlaybackBindings bindings) {
        this.bindings = bindings;
    }

    void marker(AnimationMarkerEvent event) {
        var binding = bindings.find(event.playbackId().value()).orElse(null);
        if (binding == null || !binding.animationId().equals(event.animationId().value())
                || !binding.markerId().equals(event.markerId().value())) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || !server.isSameThread()) return;
        var lookup = ItemAttachmentServiceBridge.lookup(server);
        if (lookup.status() != ItemAttachmentServiceBridge.Status.RESOLVED) return;
        MarkerSignal signal = new MarkerSignal(binding.subject(), event.animationId().value(),
                event.playbackId().value(), event.playbackRevision(), event.phase().name(),
                event.markerId().value(), event.serverTick());
        var result = lookup.service().orElseThrow().marker(binding.commandId(), signal);
        if (result.terminal()) bindings.remove(event.playbackId().value());
    }
}
