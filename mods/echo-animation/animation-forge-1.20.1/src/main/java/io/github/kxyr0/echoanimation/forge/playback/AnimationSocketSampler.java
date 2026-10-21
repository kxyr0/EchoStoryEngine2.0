package io.github.kxyr0.echoanimation.forge.playback;

import io.github.kxyr0.echoanimation.api.animation.AnimationPhase;
import io.github.kxyr0.echoanimation.api.animation.AnimationPlaybackId;
import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.animation.definition.MainMode;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketId;
import io.github.kxyr0.echoanimation.api.rig.AnimationAttachmentSocketSample;
import io.github.kxyr0.echoanimation.api.subject.AnimationSubjectId;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.core.rig.RigPoseEvaluator;
import io.github.kxyr0.echoanimation.definition.resource.AnimationResourceSet;
import io.github.kxyr0.echoanimation.forge.playback.AnimationRuntimeRecords.PlaybackRecord;

import java.util.List;
import java.util.Optional;

final class AnimationSocketSampler {
    private final NormalizedClipSampler clips = new NormalizedClipSampler();
    private final RigPoseEvaluator rigs = new RigPoseEvaluator();

    record Request( AnimationSubjectId subject, Optional<AnimationPlaybackId> expectedPlayback,
            AnimationAttachmentSocketId socket, long serverTick) {}

    Optional<AnimationAttachmentSocketSample> sample(
            AnimationRuntimeSavedData data, AnimationResourceSet resources, Request request) {
        var subject = request.subject();
        var expectedPlayback = request.expectedPlayback();
        var socket = request.socket();
        long serverTick = request.serverTick();
        List<PlaybackRecord> active = data.activePlaybacks.values().stream()
                        .filter(playback -> playback.subjectId.equals(subject)) .filter( playback ->
                                        expectedPlayback.isEmpty() || expectedPlayback .orElseThrow()
                                                        .equals(playback.playbackId))
                        .filter(playback -> playback.phase != AnimationPhase.TERMINAL) .toList();
        if (active.size() != 1) { return Optional.empty(); }
        PlaybackRecord playback = active.get(0);
        AnimationClipDefinition clip = clip(resources, playback).orElse(null);
        if (clip == null || clip.rigId().isEmpty()) { return Optional.empty(); }
        var rig = resources.animationRigs().stream() .filter(value -> value.id().equals(clip.rigId().orElseThrow()))
                        .findFirst() .orElse(null);
        if (rig == null || rig.sockets().stream().noneMatch(value -> value.id().equals(socket))) {
            return Optional.empty();
        }
        AnimationDefinition definition = resources.animations().stream()
                        .filter(value -> value.id().equals(playback.animationId)) .findFirst() .orElse(null);
        if (definition == null) { return Optional.empty(); }
        double progress = progress(playback, definition, clip);
        AnimationPose pose = clips.sample( clip, AnimationPose.defaults(rig.bones()), progress, segment(playback),
                        point(playback));
        return rigs.evaluateMatrices(rig, pose) .socket(socket) .map( matrix ->
                                new AnimationAttachmentSocketSample( subject, playback.playbackId, socket, matrix,
                                        serverTick, playback.playbackRevision));
    }

    private Optional<AnimationClipDefinition> clip( AnimationResourceSet resources, PlaybackRecord playback) {
        var id = switch (playback.phase) {
                    case ENTER -> Optional.of(playback.entryClipId);
                    case MAIN -> Optional.of(playback.mainClipId);
                    case EXIT -> playback.exitClipId;
                    case TERMINAL ->
                            Optional .<io.github.kxyr0.echoanimation.api.clip.AnimationClipId>
                                            empty();
                };
        return id.flatMap( value ->
                        resources.animationClips().stream() .filter(clip -> clip.id().equals(value)) .findFirst());
    }

    private Optional<String> segment(PlaybackRecord playback) {
        return switch (playback.phase) {
            case ENTER -> playback.entrySegmentId;
            case MAIN -> playback.mainSegmentId;
            case EXIT -> playback.exitSegmentId;
            case TERMINAL -> Optional.empty();
        };
    }

    private Optional<String> point(PlaybackRecord playback) {
        return playback.phase == AnimationPhase.MAIN ? playback.mainPointId : Optional.empty();
    }

    private double progress( PlaybackRecord playback, AnimationDefinition definition, AnimationClipDefinition clip) {
        long duration = Math.max( 1L, segment(playback) .flatMap(clip::segment)
                                .map(value -> (long) value.durationTicks()) .orElse((long) clip.durationTicks()));
        double elapsed = Math.max(0.0D, playback.sampleElapsedTicks);
        boolean loop = playback.phase == AnimationPhase.MAIN && definition.main().mode() == MainMode.LOOP_UNTIL_STOP;
        return loop ? (elapsed % duration) / duration : Math.min(1.0D, elapsed / duration);
    }
}
