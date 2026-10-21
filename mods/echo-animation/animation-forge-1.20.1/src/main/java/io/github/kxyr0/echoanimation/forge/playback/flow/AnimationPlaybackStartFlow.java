package io.github.kxyr0.echoanimation.forge.playback.flow;

import io.github.kxyr0.echoanimation.api.animation.AnimationCommandId;
import io.github.kxyr0.echoanimation.api.animation.AnimationId;
import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlayResult;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.graph.AnimationChannelId;
import io.github.kxyr0.echoanimation.api.graph.AnimationLeaseId;
import io.github.kxyr0.echoanimation.api.graph.AnimationOwnerId;
import io.github.kxyr0.echoanimation.api.graph.AnimationStateId;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.ArchivedDefinition;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.SubjectRecord;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeSavedData;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Starts high-level animations using save-before-apply semantics.
 */
public final class AnimationPlaybackStartFlow {
    private static final AnimationId LOCOMOTION_WALK = AnimationId.parse("jdh:humanoid_walk");
    private static final AnimationId LOCOMOTION_RUN = AnimationId.parse("jdh:humanoid_run");
    public StartResult start(StartRequest request) {
        Optional<AnimationDefinition> definition = request.definition();
        if (definition.isEmpty()) {
            return rejected(request, AnimationPlayResult.Status.ANIMATION_NOT_FOUND,
                    "EANIM-A3-NOT-FOUND", "Animation not found: " + request.animationId());
        }
        SubjectRecord subject = request.data().subjects.get(request.subjectId());
        if (subject == null) {
            return rejected(request, AnimationPlayResult.Status.SUBJECT_NOT_FOUND,
                    "EANIM-A3-SUBJECT", "Animation subject not found: " + request.subjectId());
        }
        List<PlaybackRecord> conflicts = conflicts(
                request.data(), request.subjectId(), definition.orElseThrow().controlledChannels());
        if (!conflicts.isEmpty()) {
            if (foregroundMayPreemptLocomotion(request.animationId(), conflicts)) {
                conflicts.forEach(playback -> preempt(request.data(), subject, playback));
            } else {
                return rejected(request, AnimationPlayResult.Status.CHANNEL_CONFLICT,
                        "EANIM-A3-CHANNEL", "One or more controlled channels are occupied.");
            }
        }
        AnimationStateId sourceState = sourceState(subject, definition.orElseThrow());
        Optional<AnimationDefinition.EntryVariant> entry = entry(definition.orElseThrow(), sourceState);
        if (!definition.orElseThrow().entryVariants().isEmpty() && entry.isEmpty()) {
            return rejected(request, AnimationPlayResult.Status.INVALID_SOURCE_STATE,
                    "EANIM-A3-SOURCE", "Animation cannot start from state: " + sourceState);
        }
        PlaybackRecord playback = playback(request, definition.orElseThrow(), entry);
        request.data().definitionArchive.put(definition.orElseThrow().fingerprint(),
                new ArchivedDefinition(definition.orElseThrow(), request.serverTick()));
        request.data().activePlaybacks.put(playback.playbackId, playback);
        definition.orElseThrow().controlledChannels().forEach(channel -> subject.channelStates.put(channel,
                phaseState(playback)));
        subject.revision++;
        request.data().setDirtyRevision();
        return new StartResult(new AnimationPlayResult(AnimationPlayResult.Status.STARTED, request.animationId(),
                request.subjectId(), Optional.of(playback.playbackId), Optional.of(playback.phase),
                "EANIM-A3-STARTED", "Started animation " + request.animationId() + "."),
                Optional.of(request.subjectId()));
    }

    private StartResult rejected(StartRequest request, AnimationPlayResult.Status status, String code, String message) {
        return new StartResult(new AnimationPlayResult(status, request.animationId(), request.subjectId(),
                Optional.empty(), Optional.empty(), code, message), Optional.empty());
    }

    private PlaybackRecord playback(
            StartRequest request,
            AnimationDefinition definition,
            Optional<AnimationDefinition.EntryVariant> entry
    ) {
        PlaybackRecord playback = new PlaybackRecord();
        playback.playbackId = AnimationPlaybackId.parse("p" + UUID.randomUUID().toString().replace("-", ""));
        playback.animationId = request.animationId();
        playback.subjectId = request.subjectId();
        playback.commandId = request.commandId();
        playback.definitionFingerprint = definition.fingerprint();
        playback.entryClipId = entry.map(AnimationDefinition.EntryVariant::clipId)
                .orElse(definition.main().clipId());
        playback.mainClipId = definition.main().clipId();
        playback.entrySegmentId = entry.flatMap(AnimationDefinition.EntryVariant::segmentId);
        playback.mainSegmentId = definition.main().segmentId();
        playback.mainPointId = definition.main().pointId();
        playback.phase = entry.isPresent() ? AnimationPhase.ENTER : AnimationPhase.MAIN;
        playback.usedAuthoredEntry = entry.isPresent();
        playback.targetState = definition.targetState();
        playback.ownerId = new AnimationOwnerId("echo_animation:" + playback.playbackId.value());
        playback.phaseEnteredTick = request.serverTick();
        playback.sampleElapsedTicks = 0.0D;
        playback.playbackSpeed = request.playbackSpeed();
        definition.controlledChannels().forEach(channel -> playback.leases.put(channel, leaseId(playback, channel)));
        return playback;
    }

    private AnimationStateId sourceState(SubjectRecord subject, AnimationDefinition definition) {
        return definition.controlledChannels().stream().sorted().findFirst()
                .map(channel -> subject.channelStates.getOrDefault(channel, initialState(definition))).orElseThrow();
    }

    private AnimationStateId initialState(AnimationDefinition definition) {
        return definition.allowedSourceStates().stream()
                .filter(state -> state.value().endsWith("/idle") || state.value().equals("idle"))
                .findFirst()
                .orElse(definition.allowedSourceStates().stream().sorted().findFirst()
                        .orElse(definition.targetState()));
    }

    private Optional<AnimationDefinition.EntryVariant> entry(AnimationDefinition definition, AnimationStateId source) {
        return definition.entryVariants().stream().filter(item -> item.sourceState().equals(source)).findFirst();
    }

    private List<PlaybackRecord> conflicts(
            AnimationRuntimeSavedData data,
            AnimationSubjectId subjectId,
            Set<AnimationChannelId> channels
    ) {
        return data.activePlaybacks.values().stream()
                .filter(playback -> playback.subjectId.equals(subjectId))
                .filter(playback -> playback.leases.keySet().stream().anyMatch(channels::contains))
                .toList();
    }

    private boolean foregroundMayPreemptLocomotion(
            AnimationId incoming,
            List<PlaybackRecord> conflicts
    ) {
        return !isLocomotion(incoming) && conflicts.stream().allMatch(playback -> isLocomotion(playback.animationId));
    }

    private boolean isLocomotion(AnimationId animationId) {
        return animationId.equals(LOCOMOTION_WALK) || animationId.equals(LOCOMOTION_RUN);
    }

    private void preempt(
            AnimationRuntimeSavedData data,
            SubjectRecord subject,
            PlaybackRecord playback
    ) {
        data.activePlaybacks.remove(playback.playbackId);
        playback.leases.keySet().forEach(channel -> subject.channelStates.put(channel, playback.targetState));
    }

    private AnimationStateId phaseState(PlaybackRecord playback) {
        return new AnimationStateId("playback/" + playback.playbackId.value() + "/"
                + playback.phase.name().toLowerCase(java.util.Locale.ROOT));
    }

    private AnimationLeaseId leaseId(PlaybackRecord playback, AnimationChannelId channel) {
        return new AnimationLeaseId("playback/" + playback.playbackId.value() + "/" + channel.value());
    }

    public record StartRequest(
            AnimationRuntimeSavedData data,
            AnimationId animationId,
            AnimationSubjectId subjectId,
            AnimationCommandId commandId,
            Optional<AnimationDefinition> definition,
            long serverTick,
            double playbackSpeed
    ) {
    }

    public record StartResult(AnimationPlayResult result, Optional<AnimationSubjectId> changedSubject) {
    }
}
