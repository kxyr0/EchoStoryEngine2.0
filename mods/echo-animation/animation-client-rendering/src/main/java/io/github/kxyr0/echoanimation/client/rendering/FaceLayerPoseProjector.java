package io.github.kxyr0.echoanimation.client.rendering;

import io.github.kxyr0.echoanimation.api.id.FaceLayerId;
import io.github.kxyr0.echoanimation.api.id.BoneId;
import io.github.kxyr0.echoanimation.api.math.IntRect;
import io.github.kxyr0.echoanimation.api.math.Transform3;
import io.github.kxyr0.echoanimation.api.math.Vec3;
import io.github.kxyr0.echoanimation.api.model.LayerRole;
import io.github.kxyr0.echoanimation.core.hierarchy.BonePose;
import io.github.kxyr0.echoanimation.core.hierarchy.BoneDefinition;
import io.github.kxyr0.echoanimation.core.transform.TransformEvaluator;
import io.github.kxyr0.echoanimation.definition.resource.FaceRigDefinition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Projects authored face layers through the evaluated face bone hierarchy.
 */
public final class FaceLayerPoseProjector {
    private static final double PIXEL = 1.0D / 16.0D;
    private static final double AXIS_EPSILON = 0.000001D;

    private final LayerDrawPlanner planner = new LayerDrawPlanner();
    private final ScleraLayerPairing scleraLayerPairing = new ScleraLayerPairing();
    private final TransformEvaluator evaluator = new TransformEvaluator();

    public List<ProjectedFaceLayer> project(
            FaceRigDefinition rig,
            BonePose pose,
            Set<FaceLayerId> hiddenLayers
    ) {
        Objects.requireNonNull(rig, "rig");
        Objects.requireNonNull(pose, "pose");
        Objects.requireNonNull(hiddenLayers, "hiddenLayers");
        var world = evaluator.evaluate(rig.hierarchy(), pose);
        List<ProjectedFaceLayer> projected = planner.plan(rig.layers(), hiddenLayers).stream()
                .map(command -> new ProjectedFaceLayer(command,
                        compose(world.get(command.boneId()).orElse(Transform3.IDENTITY), command.placement())))
                .toList();
        return clipProjectedLayers(rig, projected);
    }

    private Transform3 compose(Transform3 parent, Transform3 child) {
        Vec3 translated = parent.applyPoint(child.translation());
        return new Transform3(
                translated,
                parent.rotationZDegrees() + child.rotationZDegrees(),
                parent.scale().componentTimes(child.scale()),
                child.pivot());
    }

    private List<ProjectedFaceLayer> clipProjectedLayers(FaceRigDefinition rig, List<ProjectedFaceLayer> layers) {
        Map<BoneId, BoneDefinition> bones = rig.hierarchy().bones().stream()
                .collect(Collectors.toMap(BoneDefinition::id, bone -> bone));
        Map<BoneId, List<ProjectedFaceLayer>> byBone = new HashMap<>();
        for (ProjectedFaceLayer layer : layers) {
            byBone.computeIfAbsent(layer.command().boneId(), ignored -> new ArrayList<>()).add(layer);
        }
        Map<FaceLayerId, ProjectedFaceLayer> byLayerId = new HashMap<>();
        for (ProjectedFaceLayer layer : layers) {
            byLayerId.putIfAbsent(layer.command().layerId(), layer);
        }
        List<ProjectedFaceLayer> result = new ArrayList<>();
        for (ProjectedFaceLayer layer : layers) {
            clipToParentSurface(layer, bones, byBone, byLayerId).ifPresent(result::add);
        }
        return List.copyOf(result);
    }

    private Optional<ProjectedFaceLayer> clipToParentSurface(
            ProjectedFaceLayer layer,
            Map<BoneId, BoneDefinition> bones,
            Map<BoneId, List<ProjectedFaceLayer>> byBone,
            Map<FaceLayerId, ProjectedFaceLayer> byLayerId
    ) {
        if (layer.command().role() != LayerRole.PUPIL) {
            return Optional.of(layer);
        }
        Optional<ProjectedFaceLayer> surface = scleraLayerPairing.resolveParentSurface(layer, bones, byBone)
                .or(() -> scleraLayerPairing.resolve(layer, byLayerId));
        if (surface.isEmpty()) {
            return Optional.of(layer);
        }
        ProjectedFaceLayer candidate = surface.orElseThrow();
        if (!axisAligned(layer.transform()) || !axisAligned(candidate.transform())) {
            return Optional.empty();
        }
        return clipped(layer, candidate);
    }

    private Optional<ProjectedFaceLayer> clipped(ProjectedFaceLayer layer, ProjectedFaceLayer surface) {
        Bounds layerBounds = bounds(layer);
        Bounds clipBounds = bounds(surface);
        if (layerBounds.width() <= clipBounds.width() && layerBounds.height() <= clipBounds.height()) {
            Bounds clamped = layerBounds.withCenter(
                    clamp(layerBounds.centerX(), clipBounds.left + layerBounds.width() / 2.0D,
                            clipBounds.right - layerBounds.width() / 2.0D),
                    clamp(layerBounds.centerY(), clipBounds.bottom + layerBounds.height() / 2.0D,
                            clipBounds.top - layerBounds.height() / 2.0D));
            return Optional.of(new ProjectedFaceLayer(layer.command(), shifted(layer.transform(), clamped)));
        }
        Optional<Bounds> intersection = layerBounds.intersection(clipBounds);
        if (intersection.isEmpty()) {
            return Optional.empty();
        }
        Bounds visible = intersection.orElseThrow();
        if (visible.equals(layerBounds)) {
            return Optional.of(layer);
        }
        return clippedCommand(layer, layerBounds, visible).map(command ->
                new ProjectedFaceLayer(command, shifted(layer.transform(), visible)));
    }

    private Optional<LayerDrawCommand> clippedCommand(ProjectedFaceLayer layer, Bounds original, Bounds visible) {
        IntRect atlas = layer.command().atlasRect();
        int x0 = atlas.x() + cropStart(original.left(), original.width(), visible.left(), atlas.width());
        int y0 = atlas.y() + cropStart(original.bottom(), original.height(), visible.bottom(), atlas.height());
        int x1 = atlas.x() + cropEnd(original.left(), original.width(), visible.right(), atlas.width());
        int y1 = atlas.y() + cropEnd(original.bottom(), original.height(), visible.top(), atlas.height());
        if (x1 <= x0 || y1 <= y0) {
            return Optional.empty();
        }
        return Optional.of(new LayerDrawCommand(layer.command().layerId(), layer.command().boneId(),
                layer.command().textureId(), layer.command().role(), layer.command().placement(),
                new IntRect(x0, y0, x1 - x0, y1 - y0), layer.command().clipRect()));
    }

    private int cropStart(double originalStart, double originalSize, double visibleStart, int pixels) {
        return (int) Math.floor(((visibleStart - originalStart) / originalSize) * pixels);
    }

    private int cropEnd(double originalStart, double originalSize, double visibleEnd, int pixels) {
        return (int) Math.ceil(((visibleEnd - originalStart) / originalSize) * pixels);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private Transform3 shifted(Transform3 transform, Bounds visible) {
        return new Transform3(new Vec3(visible.centerX(), visible.centerY(), transform.translation().z()),
                transform.rotationZDegrees(), transform.scale(), transform.pivot());
    }

    private Bounds bounds(ProjectedFaceLayer layer) {
        IntRect atlas = layer.command().atlasRect();
        Transform3 transform = layer.transform();
        double width = atlas.width() * PIXEL * Math.abs(transform.scale().x());
        double height = atlas.height() * PIXEL * Math.abs(transform.scale().y());
        double x = transform.translation().x();
        double y = transform.translation().y();
        return new Bounds(x - width / 2.0D, x + width / 2.0D, y - height / 2.0D, y + height / 2.0D);
    }

    private boolean axisAligned(Transform3 transform) {
        double angle = Math.abs(transform.rotationZDegrees() % 180.0D);
        return angle < AXIS_EPSILON || Math.abs(angle - 180.0D) < AXIS_EPSILON;
    }

    private record Bounds(double left, double right, double bottom, double top) {
        Optional<Bounds> intersection(Bounds other) {
            double nextLeft = Math.max(left, other.left);
            double nextRight = Math.min(right, other.right);
            double nextBottom = Math.max(bottom, other.bottom);
            double nextTop = Math.min(top, other.top);
            if (nextRight <= nextLeft || nextTop <= nextBottom) {
                return Optional.empty();
            }
            return Optional.of(new Bounds(nextLeft, nextRight, nextBottom, nextTop));
        }

        double width() {
            return right - left;
        }

        double height() {
            return top - bottom;
        }

        double centerX() {
            return (left + right) / 2.0D;
        }

        double centerY() {
            return (bottom + top) / 2.0D;
        }

        Bounds withCenter(double x, double y) {
            double halfWidth = width() / 2.0D;
            double halfHeight = height() / 2.0D;
            return new Bounds(x - halfWidth, x + halfWidth, y - halfHeight, y + halfHeight);
        }
    }
}
