package io.github.kxyr0.echoanimation.forge.playback.marker;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerOccurrence;
import io.github.kxyr0.echoanimation.forge.api.marker.AnimationMarkerEvent;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Persists exactly-once playback receipts before publishing public server events.
 */
public final class AnimationMarkerDeliveryFlow {
    private final Consumer<AnimationMarkerEvent> consumer;

    public AnimationMarkerDeliveryFlow(Consumer<AnimationMarkerEvent> consumer) {
        this.consumer = Objects.requireNonNull(consumer, "consumer");
    }

    public void deliver(AnimationRuntimeSavedData data, PlaybackRecord playback,
            AnimationDefinition definition, long serverTick) {
        for (AnimationMarkerDefinition marker : definition.markers()) {
            if (marker.phase() != playback.phase || marker.phaseTick() != playback.phaseElapsedTicks
                    || !playback.deliveredMarkers.add(marker.id())) {
                continue;
            }
            playback.playbackRevision++;
            data.setDirtyRevision();
            consumer.accept(new AnimationMarkerEvent(new AnimationMarkerOccurrence(
                    playback.subjectId, playback.animationId, playback.playbackId, playback.ownerId,
                    marker.id(), marker.phase(), marker.phaseTick(), serverTick, playback.playbackRevision)));
        }
    }
}
