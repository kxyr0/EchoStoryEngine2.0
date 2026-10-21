package io.github.kxyr0.echoanimation.forge.api.marker;

import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerOccurrence;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.Objects;
import net.minecraftforge.eventbus.api.Event;

/**
 * Server-thread notification for one durable animation marker occurrence.
 */
public final class AnimationMarkerEvent extends Event {
    private final AnimationMarkerOccurrence occurrence;

    public AnimationMarkerEvent(AnimationMarkerOccurrence occurrence) {
        this.occurrence = Objects.requireNonNull(occurrence, "occurrence");
    }

    public AnimationMarkerOccurrence occurrence() { return occurrence; }
    public AnimationSubjectId subjectId() { return occurrence.subjectId(); }
    public AnimationId animationId() { return occurrence.animationId(); }
    public AnimationPlaybackId playbackId() { return occurrence.playbackId(); }
    public AnimationOwnerId ownerId() { return occurrence.ownerId(); }
    public AnimationMarkerId markerId() { return occurrence.markerId(); }
    public AnimationPhase phase() { return occurrence.phase(); }
    public long phaseTick() { return occurrence.phaseTick(); }
    public long serverTick() { return occurrence.serverTick(); }
    public long playbackRevision() { return occurrence.playbackRevision(); }
    public AnimationMarkerAuthority authority() { return AnimationMarkerAuthority.SERVER; }
}
