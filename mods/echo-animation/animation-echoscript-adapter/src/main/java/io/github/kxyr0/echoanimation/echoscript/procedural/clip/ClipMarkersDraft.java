package io.github.kxyr0.echoanimation.echoscript.procedural.clip;

import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipEvent;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationClipPoint;
import io.github.kxyr0.echoanimation.api.clip.definition.AnimationRootMotion;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.echoscript.procedural.ProceduralSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Accumulates points, events and root-motion metadata outside the track draft. */
final class ClipMarkersDraft {
    private final List<AnimationClipPoint> points = new ArrayList<>();
    private final List<AnimationClipEvent> events = new ArrayList<>();
    private AnimationRootMotion rootMotion = AnimationRootMotion.disabled();
    private boolean rootMotionDeclared;

    void point(InvocationExpressionSyntax call, Map<String, ClipSegmentDraft> segments) {
        ExpressionSyntax first = ProceduralSyntax.argument(call, 0, null);
        Optional<ClipSegmentDraft> owner = first instanceof NameExpressionSyntax name
                ? Optional.ofNullable(segments.get(name.name())) : Optional.empty();
        String pointId = owner.isPresent() ? ProceduralSyntax.string(call, 1, null)
                : ProceduralSyntax.string(call, 0, null);
        int tick = owner.map(segment -> pointTick(segment, ProceduralSyntax.argument(call, 2, "at")))
                .orElseGet(() -> ProceduralSyntax.integer(call, 1, "at"));
        requireUnique(points.stream().map(AnimationClipPoint::id).toList(), pointId, "point");
        points.add(new AnimationClipPoint(pointId, tick, owner.map(segment -> segment.id)));
    }

    void event(InvocationExpressionSyntax call) {
        String id = ProceduralSyntax.string(call, 0, null);
        int tick = ProceduralSyntax.integer(call, 1, "at");
        String payload = ProceduralSyntax.optional(call, 2, "payload").map(expression -> {
            if (expression instanceof StringLiteralExpressionSyntax string) {
                return string.value();
            }
            throw new IllegalArgumentException("Event payload must be a string.");
        }).orElse("");
        if (events.stream().anyMatch(event -> event.id().equals(id) && event.tick() == tick)) {
            throw new IllegalArgumentException("Duplicate event '" + id + "' at tick " + tick + ".");
        }
        events.add(new AnimationClipEvent(id, tick, payload));
    }

    void rootMotion(InvocationExpressionSyntax call) {
        if (rootMotionDeclared) {
            throw new IllegalArgumentException("RootMotion is declared twice.");
        }
        AnimationRootMotion.Mode mode = AnimationRootMotion.Mode.valueOf(
                ProceduralSyntax.name(ProceduralSyntax.argument(call, 0, null)).toUpperCase());
        String bone = ProceduralSyntax.string(call, 1, "bone");
        rootMotion = new AnimationRootMotion(mode, BoneId.parse(bone));
        rootMotionDeclared = true;
    }

    List<AnimationClipPoint> points() {
        return List.copyOf(points);
    }

    List<AnimationClipEvent> events() {
        return List.copyOf(events);
    }

    AnimationRootMotion rootMotion() {
        return rootMotion;
    }

    int authoredEnd() {
        int pointEnd = points.stream().mapToInt(AnimationClipPoint::tick).max().orElse(0);
        int eventEnd = events.stream().mapToInt(AnimationClipEvent::tick).max().orElse(0);
        return Math.max(pointEnd, eventEnd);
    }

    private int pointTick(ClipSegmentDraft segment, ExpressionSyntax expression) {
        if (expression instanceof NameExpressionSyntax name) {
            return switch (name.name().toUpperCase()) {
                case "START" -> segment.start;
                case "END" -> segment.end;
                default -> throw new IllegalArgumentException("Unknown point symbol: " + name.name());
            };
        }
        double progress = ProceduralSyntax.number(expression);
        if (progress < 0.0D || progress > 1.0D) {
            throw new IllegalArgumentException("Point progress must be in range 0..1.");
        }
        return segment.start + (int) Math.round(progress * (segment.end - segment.start));
    }

    private void requireUnique(List<String> ids, String id, String kind) {
        if (ids.contains(id)) {
            throw new IllegalArgumentException("Duplicate " + kind + " '" + id + "'.");
        }
    }
}
