package io.github.kxyr0.echoanimation.echoscript.procedural.clip;

import io.github.kxyr0.echoanimation.api.clip.AnimationClipId;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationBoneTrackDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipDefinition;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipSegment;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationKeyframeDefinition;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.model.AnimationLoopSeam;
import io.github.kxyr0.echoanimation.api.rig.AnimationRigId;
import io.github.kxyr0.echoanimation.definition.fingerprint.SemanticFingerprinter;
import io.github.kxyr0.echoanimation.echoscript.procedural.ProceduralSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Order-stable accumulator for one procedural AnimationClipResource Build method.
 */
final class ClipDslDraft {
    private String id;
    private String rig;
    private int explicitDuration = -1;
    private final Map<String, ClipSegmentDraft> segments = new LinkedHashMap<>();
    private final Map<String, String> tracks = new LinkedHashMap<>();
    private final Map<String, List<ClipKeyReader.KeyDraft>> keys = new LinkedHashMap<>();
    private final ClipMarkersDraft markers = new ClipMarkersDraft();

    void id(InvocationExpressionSyntax call) {
        requireUnset(id, "Id");
        id = ProceduralSyntax.string(call, 0, null);
    }

    void rig(InvocationExpressionSyntax call) {
        requireUnset(rig, "Rig");
        rig = ProceduralSyntax.string(call, 0, null);
    }

    void duration(InvocationExpressionSyntax call) {
        if (explicitDuration >= 0) {
            throw new IllegalArgumentException("Duration is declared twice.");
        }
        explicitDuration = ProceduralSyntax.integer(call, 0, null);
    }

    void segment(String variable, InvocationExpressionSyntax call) {
        String segmentId = ProceduralSyntax.string(call, 0, null);
        int start = ProceduralSyntax.integer(call, 1, "from");
        int end = ProceduralSyntax.integer(call, 2, "to");
        if (segments.values().stream().anyMatch(segment -> segment.id.equals(segmentId))) {
            throw new IllegalArgumentException("Duplicate segment '" + segmentId + "'.");
        }
        putUnique(segments, variable, new ClipSegmentDraft(segmentId, start, end));
    }

    void track(String variable, InvocationExpressionSyntax call) {
        String trackId = ProceduralSyntax.string(call, 0, null);
        if (tracks.containsValue(trackId)) {
            throw new IllegalArgumentException("Duplicate track '" + trackId + "'.");
        }
        putUnique(tracks, variable, trackId);
        keys.put(trackId, new ArrayList<>());
    }

    void point(InvocationExpressionSyntax call) {
        markers.point(call, segments);
    }

    void event(InvocationExpressionSyntax call) {
        markers.event(call);
    }

    void rootMotion(InvocationExpressionSyntax call) {
        markers.rootMotion(call);
    }

    void loop(InvocationExpressionSyntax call) {
        ClipSegmentDraft segment = referencedSegment(call);
        String seam = ProceduralSyntax.optional(call, 1, "seam").map(ProceduralSyntax::name).orElse("Smooth");
        if (segment.loopDeclared) {
            throw new IllegalArgumentException("Segment '" + segment.id + "' has two Loop declarations.");
        }
        segment.loopDeclared = true;
        segment.seam = AnimationLoopSeam.valueOf(seam.toUpperCase());
    }

    void speed(InvocationExpressionSyntax call) {
        ClipSegmentDraft segment = referencedSegment(call);
        segment.speed = ProceduralSyntax.number(call, 1, "blocksPerSecond");
    }

    void key(ClipKeyReader.KeyDraft key) {
        if (key.tick() < 0) {
            throw new IllegalArgumentException("Key tick must not be negative.");
        }
        List<ClipKeyReader.KeyDraft> trackKeys = keys.get(key.track());
        Optional<ClipKeyReader.KeyDraft> duplicate = trackKeys.stream()
                .filter(existing -> existing.tick() == key.tick()).findFirst();
        if (duplicate.isPresent() && !duplicate.orElseThrow().equals(key)) {
            throw new IllegalArgumentException("Track '" + key.track() + "' contains two keys at tick "
                    + key.tick() + " with conflicting transforms.");
        }
        if (duplicate.isEmpty()) {
            trackKeys.add(key);
        }
    }

    Map<String, String> tracks() {
        return tracks;
    }

    AnimationClipDefinition build() {
        if (id == null || rig == null || tracks.isEmpty()) {
            throw new IllegalArgumentException("Procedural clip requires Id, Rig and at least one Track.");
        }
        int authoredEnd = authoredEnd();
        int duration = explicitDuration < 0 ? authoredEnd : explicitDuration;
        if (duration <= 0 || authoredEnd > duration) {
            throw new IllegalArgumentException("Duration " + duration + " does not cover authored tick "
                    + authoredEnd + ".");
        }
        List<AnimationClipSegment> compiledSegments = segments.values().stream()
                .map(ClipSegmentDraft::compile).toList();
        compiledSegments.stream().filter(segment -> segment.endTick() > duration).findFirst().ifPresent(segment -> {
            throw new IllegalArgumentException("Segment '" + segment.id() + "' exceeds clip duration " + duration);
        });
        List<AnimationBoneTrackDefinition> compiledTracks = tracks.values().stream()
                .map(track -> compileTrack(track, duration)).toList();
        AnimationLoopSeam seam = compiledSegments.stream().filter(segment -> segment.loopSeam()
                != AnimationLoopSeam.HOLD).map(AnimationClipSegment::loopSeam).findFirst()
                .orElse(AnimationLoopSeam.HOLD);
        double speed = compiledSegments.stream().mapToDouble(AnimationClipSegment::authoredSpeedBlocksPerSecond)
                .filter(value -> value != 1.0D).findFirst().orElse(1.0D);
        String fingerprint = new SemanticFingerprinter().fingerprint(List.of(id, rig, Integer.toString(duration),
                compiledSegments.toString(), markers.points().toString(), markers.events().toString(),
                markers.rootMotion().toString(), compiledTracks.toString()));
        return new AnimationClipDefinition(AnimationClipId.parse(id), Optional.of(AnimationRigId.parse(rig)),
                compiledTracks, duration, compiledSegments, markers.points(), markers.events(), markers.rootMotion(),
                3, seam, speed, fingerprint);
    }

    private AnimationBoneTrackDefinition compileTrack(String track, int duration) {
        List<AnimationKeyframeDefinition> frames = keys.get(track).stream()
                .sorted(Comparator.comparingInt(ClipKeyReader.KeyDraft::tick))
                .map(key -> new AnimationKeyframeDefinition(key.tick() / (double) duration, key.position(),
                        key.rotation(), key.scale(), key.interpolation())).toList();
        return new AnimationBoneTrackDefinition(BoneId.parse(track), frames);
    }

    private int authoredEnd() {
        int keyEnd = keys.values().stream().flatMap(List::stream).mapToInt(ClipKeyReader.KeyDraft::tick).max()
                .orElse(0);
        int segmentEnd = segments.values().stream().mapToInt(segment -> segment.end).max().orElse(0);
        return Math.max(keyEnd, Math.max(segmentEnd, markers.authoredEnd()));
    }

    private ClipSegmentDraft referencedSegment(InvocationExpressionSyntax call) {
        return segment(ProceduralSyntax.name(ProceduralSyntax.argument(call, 0, null)));
    }

    private ClipSegmentDraft segment(String variable) {
        return Optional.ofNullable(segments.get(variable))
                .orElseThrow(() -> new IllegalArgumentException("Unknown segment variable '" + variable + "'."));
    }

    private void requireUnset(String value, String name) {
        if (value != null) {
            throw new IllegalArgumentException(name + " is declared twice.");
        }
    }

    private <T> void putUnique(Map<String, T> values, String variable, T value) {
        if (values.putIfAbsent(variable, value) != null) {
            throw new IllegalArgumentException("Local variable '" + variable + "' is declared twice.");
        }
    }

}
