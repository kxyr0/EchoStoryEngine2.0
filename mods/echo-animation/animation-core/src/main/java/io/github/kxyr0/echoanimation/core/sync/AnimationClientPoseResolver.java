package io.github.kxyr0.echoanimation.core.sync;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.AnimationChannelDefinition;
import io.github.kxyr0.echoanimation.api.graph.definition.PlaybackMode;
import io.github.kxyr0.echoanimation.api.rig.definition.AnimationRigDefinition;
import io.github.kxyr0.echoanimation.api.sync.AnimationChannelSyncState;
import io.github.kxyr0.echoanimation.api.sync.AnimationSubjectSyncSnapshot;
import io.github.kxyr0.echoanimation.core.playback.AnimationPose;
import io.github.kxyr0.echoanimation.core.playback.ChannelPoseComposition;
import io.github.kxyr0.echoanimation.core.playback.NormalizedClipSampler;
import io.github.kxyr0.echoanimation.core.playback.PoseBlender;
import io.github.kxyr0.echoanimation.core.playback.SampledChannelPose;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resolves authoritative client sync cursors into a renderable local rig pose.
 */
public final class AnimationClientPoseResolver {
    private final NormalizedClipSampler sampler = new NormalizedClipSampler();
    private final PoseBlender blender = new PoseBlender();
    private final ChannelPoseComposition composition = new ChannelPoseComposition();

    public AnimationPose resolve(
            AnimationRigDefinition rig,
            AnimationSubjectSyncSnapshot snapshot,
            List<AnimationClipDefinition> clips
    ) {
        return resolve(rig, snapshot, clips, snapshot.authoritativeTick());
    }

    public AnimationPose resolve(
            AnimationRigDefinition rig,
            AnimationSubjectSyncSnapshot snapshot,
            List<AnimationClipDefinition> clips,
            long sampleTick
    ) {
        return resolve(rig, snapshot, clips, (double) sampleTick);
    }

    public AnimationPose resolve(
            AnimationRigDefinition rig,
            AnimationSubjectSyncSnapshot snapshot,
            List<AnimationClipDefinition> clips,
            double sampleTick
    ) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(clips, "clips");
        AnimationPose base = AnimationPose.defaults(rig.bones());
        ResolveInput input = new ResolveInput(rig, base, clips, snapshot, sampleTick);
        List<SampledChannelPose> channels = new ArrayList<>();
        for (AnimationChannelSyncState channel : snapshot.channels()) {
            sampleChannel(input, channel).ifPresent(channels::add);
        }
        return composition.compose(base, channels);
    }

    private Optional<SampledChannelPose> sampleChannel(ResolveInput input, AnimationChannelSyncState channel) {
        Optional<AnimationClipDefinition> currentClip = channel.clipId()
                .flatMap(id -> findClip(input.rig(), input.clips(), id));
        if (currentClip.isEmpty()) {
            return Optional.empty();
        }
        double progress = advancedProgress(channel, input.snapshot(), input.sampleTick());
        AnimationPose current = sampler.sample(currentClip.orElseThrow(), input.base(), progress,
                channel.segmentId(), channel.pointId());
        AnimationPose pose = blendTransition(input, channel, current, progress);
        AnimationChannelDefinition definition = new AnimationChannelDefinition(
                channel.channelId(),
                channel.stateId(),
                channel.visualOrder(),
                channel.compositionPolicy(),
                channel.boneMask());
        return Optional.of(new SampledChannelPose(definition, pose, channel.weight()));
    }

    private AnimationPose blendTransition(ResolveInput input, AnimationChannelSyncState channel,
            AnimationPose current, double progress) {
        if (channel.previousClipId().isEmpty() || channel.transitionProgress() >= 1.0D) {
            return current;
        }
        Optional<AnimationClipDefinition> previousClip = findClip(input.rig(), input.clips(),
                channel.previousClipId().orElseThrow());
        if (previousClip.isEmpty()) {
            return current;
        }
        AnimationPose previous = sampler.sample(previousClip.orElseThrow(), input.base(), progress);
        return blender.blend(previous, current,
                advancedTransitionProgress(channel, input.snapshot(), input.sampleTick()));
    }

    private double advancedTransitionProgress(
            AnimationChannelSyncState channel,
            AnimationSubjectSyncSnapshot snapshot,
            double sampleTick
    ) {
        if (channel.transitionDurationTicks() <= 0L) {
            return channel.transitionProgress();
        }
        double delta = Math.max(0.0D, sampleTick - snapshot.authoritativeTick());
        double elapsed = channel.transitionElapsedTicks() + delta;
        return Math.min(1.0D, elapsed / (double) channel.transitionDurationTicks());
    }

    private double advancedProgress(
            AnimationChannelSyncState channel,
            AnimationSubjectSyncSnapshot snapshot,
            double sampleTick
    ) {
        double delta = Math.max(0.0D, sampleTick - snapshot.authoritativeTick());
        if (channel.playbackMode() == PlaybackMode.LOOP) {
            double advanced = channel.normalizedProgress()
                    + delta * channel.playbackSpeed() / (double) channel.durationTicks();
            return advanced - Math.floor(advanced);
        }
        return Math.min(1.0D, channel.normalizedProgress()
                + delta * channel.playbackSpeed() / (double) channel.durationTicks());
    }

    private Optional<AnimationClipDefinition> findClip(
            AnimationRigDefinition rig,
            List<AnimationClipDefinition> clips,
            AnimationClipId id
    ) {
        return clips.stream()
                .filter(clip -> clip.id().equals(id))
                .filter(clip -> clip.rigId().isEmpty() || clip.rigId().orElseThrow().equals(rig.id()))
                .findFirst();
    }

    private record ResolveInput(
            AnimationRigDefinition rig,
            AnimationPose base,
            List<AnimationClipDefinition> clips,
            AnimationSubjectSyncSnapshot snapshot,
            double sampleTick
    ) {
    }
}
