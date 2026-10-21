package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackOutcome;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.marker.AnimationMarkerId;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileFingerprint;
import io.github.kxyr0.echoanimation.api.profile.AnimationProfileId;
import io.github.kxyr0.echoanimation.api.provisioning.AnimationSubjectProvenance;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Mutable server-thread records persisted by Forge SavedData.
 */
public final class AnimationRuntimeRecords {
    private AnimationRuntimeRecords() {
    }

    public static final class SubjectRecord {
        public final AnimationSubjectId subjectId;
        public final Map<AnimationChannelId, AnimationStateId> channelStates = new LinkedHashMap<>();
        public Optional<AnimationSubjectProvenance> provenance = Optional.empty();
        public Optional<AnimationProfileId> profileId = Optional.empty();
        public Optional<AnimationProfileFingerprint> profileFingerprint = Optional.empty();
        public Optional<AnimationProfileId> pendingProfileId = Optional.empty();
        public Optional<AnimationProfileFingerprint> pendingProfileFingerprint = Optional.empty();
        public long generation = 1L;
        public long revision = 1L;

        public SubjectRecord(AnimationSubjectId subjectId) {
            this.subjectId = subjectId;
        }
    }

    public static final class PlaybackRecord {
        public AnimationPlaybackId playbackId;
        public AnimationId animationId;
        public AnimationSubjectId subjectId;
        public AnimationCommandId commandId;
        public String definitionFingerprint;
        public AnimationClipId entryClipId;
        public AnimationClipId mainClipId;
        public Optional<AnimationClipId> exitClipId = Optional.empty();
        public Optional<String> entrySegmentId = Optional.empty();
        public Optional<String> mainSegmentId = Optional.empty();
        public Optional<String> exitSegmentId = Optional.empty();
        public Optional<String> mainPointId = Optional.empty();
        public AnimationPhase phase = AnimationPhase.ENTER;
        public long phaseEnteredTick;
        public long phaseElapsedTicks;
        public double sampleElapsedTicks = -1.0D;
        public long mainLoopIteration;
        public double playbackSpeed = 1.0D;
        public boolean exitRequested;
        public boolean usedAuthoredEntry;
        public boolean fallbackExit;
        public Optional<String> selectedExitPoint = Optional.empty();
        public AnimationStateId targetState;
        public long playbackRevision = 1L;
        public AnimationOwnerId ownerId;
        public final Map<AnimationChannelId, AnimationLeaseId> leases = new LinkedHashMap<>();
        public final Set<AnimationMarkerId> deliveredMarkers = new LinkedHashSet<>();
    }

    public static final class CommandRecord {
        public AnimationCommandId commandId;
        public String operation;
        public String argumentsFingerprint;
        public String status;
        public Optional<AnimationPlaybackId> playbackId = Optional.empty();
        public String diagnosticCode;
        public String message;
    }

    public static final class TerminalRecord {
        public AnimationPlaybackId playbackId;
        public AnimationId animationId;
        public AnimationSubjectId subjectId;
        public AnimationPlaybackOutcome outcome;
        public long finalRevision;
        public long completionTick;
        public AnimationCommandId commandId;
    }

    public record ArchivedDefinition(AnimationDefinition definition, long storedAtTick) {
    }
}
