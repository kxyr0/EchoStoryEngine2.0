package io.github.kxyr0.echoik.debug;

import io.github.kxyr0.echoik.core.model.Pose;
import io.github.kxyr0.echoik.core.solver.IkResult;
import io.github.kxyr0.echoik.core.solver.IkTarget;
import io.github.kxyr0.echoik.core.solver.SolverContext;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds debug primitives for bones, goals, pole targets and solver diagnostics.
 */
public final class IkDebugFrameBuilder {
    public List<DebugPrimitive> build(SolverContext context, IkResult result) {
        List<DebugPrimitive> primitives = new ArrayList<>();
        Pose pose = result.pose();
        IkTarget target = context.target();
        var root = pose.requireTransform(target.rootBoneId());
        var mid = pose.requireTransform(target.midBoneId());
        var end = pose.requireTransform(target.endBoneId());
        primitives.add(new DebugPrimitive.Line(root.translation(), mid.translation(), "upper"));
        primitives.add(new DebugPrimitive.Line(mid.translation(), end.translation(), "lower"));
        primitives.add(new DebugPrimitive.Point(target.targetPosition(), "target"));
        primitives.add(new DebugPrimitive.Axis(end.translation(), end.rotation(), 0.25D, "end-axis"));
        target.poleTarget().ifPresent(pole -> {
            primitives.add(new DebugPrimitive.Point(pole, "pole"));
            primitives.add(new DebugPrimitive.Line(root.translation(), pole, "pole-line"));
        });
        result.diagnostics().forEach(message ->
                primitives.add(new DebugPrimitive.Text(end.translation(), message)));
        return List.copyOf(primitives);
    }
}
