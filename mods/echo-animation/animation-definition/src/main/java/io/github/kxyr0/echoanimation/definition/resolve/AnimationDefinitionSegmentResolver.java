package io.github.kxyr0.echoanimation.definition.resolve;

import io.github.kxyr0.echoanimation.api.animation.definition.AnimationDefinition;
import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipPoint;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves lightweight segment/point references after the whole resource catalog is known.
 */
public final class AnimationDefinitionSegmentResolver {
    public AnimationDefinition resolve(AnimationDefinition definition, List<AnimationClipDefinition> clips) {
        Map<AnimationClipId, AnimationClipDefinition> byId = clips.stream()
                .collect(Collectors.toMap(AnimationClipDefinition::id, Function.identity(), (left, right) -> left));
        List<AnimationDefinition.EntryVariant> entries = definition.entryVariants().stream()
                .map(entry -> entry(entry, byId)).toList();
        AnimationDefinition.MainPhase main = main(definition.main(), byId);
        List<AnimationDefinition.ExitVariant> exits = definition.exitVariants().stream()
                .map(exit -> exit(exit, byId)).toList();
        return new AnimationDefinition(definition.id(), definition.controlledChannels(),
                definition.allowedSourceStates(), entries, main, exits, definition.markers(), definition.targetState(),
                definition.recoveryPolicy(), definition.checkpointIntervalTicks(), definition.replayPolicy(),
                definition.blendPolicy(), definition.fingerprint());
    }

    private AnimationDefinition.EntryVariant entry(AnimationDefinition.EntryVariant entry,
            Map<AnimationClipId, AnimationClipDefinition> clips) {
        long duration = duration(clip(clips, entry.clipId()), entry.segmentId(), entry.durationTicks());
        return new AnimationDefinition.EntryVariant(entry.sourceState(), entry.clipId(), entry.segmentId(), duration);
    }

    private AnimationDefinition.MainPhase main(AnimationDefinition.MainPhase main,
            Map<AnimationClipId, AnimationClipDefinition> clips) {
        AnimationClipDefinition clip = clip(clips, main.clipId());
        long duration = duration(clip, main.segmentId(), main.durationTicks());
        main.pointId().ifPresent(point -> requirePoint(clip, point));
        List<AnimationDefinition.ExitPoint> points = main.exitPoints().stream()
                .map(point -> point(point, clip, main.segmentId())).toList();
        return new AnimationDefinition.MainPhase(main.clipId(), main.segmentId(), main.pointId(), main.mode(),
                duration, points);
    }

    private AnimationDefinition.ExitVariant exit(AnimationDefinition.ExitVariant exit,
            Map<AnimationClipId, AnimationClipDefinition> clips) {
        long duration = duration(clip(clips, exit.clipId()), exit.segmentId(), exit.durationTicks());
        return new AnimationDefinition.ExitVariant(exit.exitPointId(), exit.clipId(), exit.segmentId(), duration);
    }

    private AnimationDefinition.ExitPoint point(AnimationDefinition.ExitPoint point,
            AnimationClipDefinition clip, Optional<String> segmentId) {
        if (point.sourcePointId().isEmpty()) {
            return point;
        }
        AnimationClipPoint source = requirePoint(clip, point.sourcePointId().orElseThrow());
        double progress;
        if (segmentId.isPresent()) {
            AnimationClipSegment segment = requireSegment(clip, segmentId.orElseThrow());
            if (source.tick() < segment.startTick() || source.tick() > segment.endTick()) {
                throw new IllegalArgumentException("Point '" + source.id() + "' is outside segment '"
                        + segment.id() + "'.");
            }
            progress = (source.tick() - segment.startTick()) / (double) segment.durationTicks();
        } else {
            progress = source.tick() / (double) requireDuration(clip);
        }
        return new AnimationDefinition.ExitPoint(point.id(), progress, point.sourcePointId());
    }

    private long duration(AnimationClipDefinition clip, Optional<String> segmentId, long authored) {
        if (segmentId.isPresent()) {
            return requireSegment(clip, segmentId.orElseThrow()).durationTicks();
        }
        if (authored > 0L) {
            return authored;
        }
        return requireDuration(clip);
    }

    private int requireDuration(AnimationClipDefinition clip) {
        if (clip.durationTicks() <= 0) {
            throw new IllegalArgumentException("Clip '" + clip.id() + "' has no authored duration.");
        }
        return clip.durationTicks();
    }

    private AnimationClipDefinition clip(Map<AnimationClipId, AnimationClipDefinition> clips, AnimationClipId id) {
        return Optional.ofNullable(clips.get(id))
                .orElseThrow(() -> new IllegalArgumentException("Unknown animation clip: " + id));
    }

    private AnimationClipSegment requireSegment(AnimationClipDefinition clip, String id) {
        return clip.segment(id).orElseThrow(() -> new IllegalArgumentException("Clip '" + clip.id()
                + "' does not contain segment '" + id + "'."));
    }

    private AnimationClipPoint requirePoint(AnimationClipDefinition clip, String id) {
        return clip.point(id).orElseThrow(() -> new IllegalArgumentException("Clip '" + clip.id()
                + "' does not contain point '" + id + "'."));
    }
}
