package io.github.kxyr0.echoik.forge.runtime;

import io.github.kxyr0.echoik.core.math.Vec3;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * Runtime-only IK constraints keyed by Echo Animation subject id.
 */
public final class IkRuntimeState {
    private final Map<String, IkConstraint> constraints = new HashMap<>();
    private final Map<String, Boolean> debugFlags = new HashMap<>();
    private final int blendDurationTicks;

    public IkRuntimeState(int blendDurationTicks) {
        this.blendDurationTicks = Math.max(0, blendDurationTicks);
    }

    public IkConstraintSnapshot setTarget(String subjectId, Vec3 target) {
        IkConstraint previous = constraints.get(subjectId);
        double current = previous == null ? 0.0D : previous.currentWeight();
        double targetWeight = previous == null ? 1.0D : previous.targetWeight();
        Optional<Vec3> pole = previous == null ? Optional.empty() : previous.poleTarget();
        IkConstraint next = IkConstraint.blendTo(subjectId, target, pole, current, targetWeight,
                false, blendDurationTicks);
        constraints.put(subjectId, next);
        return snapshot(subjectId, next);
    }

    public Optional<IkConstraintSnapshot> setPole(String subjectId, Vec3 pole) {
        IkConstraint previous = constraints.get(subjectId);
        if (previous == null) {
            return Optional.empty();
        }
        IkConstraint next = IkConstraint.blendTo(subjectId, previous.targetPosition(), Optional.of(pole),
                previous.currentWeight(), previous.targetWeight(), false, blendDurationTicks);
        constraints.put(subjectId, next);
        return Optional.of(snapshot(subjectId, next));
    }

    public Optional<IkConstraintSnapshot> setWeight(String subjectId, double weight) {
        IkConstraint previous = constraints.get(subjectId);
        if (previous == null) {
            return Optional.empty();
        }
        IkConstraint next = IkConstraint.blendTo(subjectId, previous.targetPosition(), previous.poleTarget(),
                previous.currentWeight(), weight, false, blendDurationTicks);
        constraints.put(subjectId, next);
        return Optional.of(snapshot(subjectId, next));
    }

    public Optional<IkConstraintSnapshot> clear(String subjectId) {
        IkConstraint previous = constraints.get(subjectId);
        if (previous == null) {
            return Optional.empty();
        }
        IkConstraint next = IkConstraint.blendTo(subjectId, previous.targetPosition(), previous.poleTarget(),
                previous.currentWeight(), 0.0D, true, blendDurationTicks);
        constraints.put(subjectId, next);
        return Optional.of(snapshot(subjectId, next));
    }

    public IkConstraintSnapshot setDebug(String subjectId, boolean enabled) {
        debugFlags.put(subjectId, enabled);
        IkConstraint constraint = constraints.get(subjectId);
        return constraint == null ? emptySnapshot(subjectId) : snapshot(subjectId, constraint);
    }

    public void apply(IkConstraintSnapshot snapshot) {
        debugFlags.put(snapshot.subjectId(), snapshot.debugEnabled());
        if (snapshot.targetPosition().isEmpty()) {
            return;
        }
        IkConstraint previous = constraints.get(snapshot.subjectId());
        double current = previous == null ? 0.0D : previous.currentWeight();
        IkConstraint next = IkConstraint.blendTo(snapshot.subjectId(), snapshot.targetPosition().orElseThrow(),
                snapshot.poleTarget(), current, snapshot.targetWeight(), snapshot.clearWhenZero(),
                blendDurationTicks);
        constraints.put(snapshot.subjectId(), next);
    }

    public Optional<IkConstraint> constraint(String subjectId) {
        return Optional.ofNullable(constraints.get(subjectId));
    }

    public boolean debugEnabled(String subjectId) {
        return debugFlags.getOrDefault(subjectId, false);
    }

    public void tick() {
        Iterator<Map.Entry<String, IkConstraint>> iterator = constraints.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, IkConstraint> entry = iterator.next();
            IkConstraint next = entry.getValue().tick();
            if (next.finishedClear()) {
                iterator.remove();
            } else {
                entry.setValue(next);
            }
        }
    }

    private IkConstraintSnapshot snapshot(String subjectId, IkConstraint constraint) {
        return new IkConstraintSnapshot(subjectId, Optional.of(constraint.targetPosition()),
                constraint.poleTarget(), constraint.targetWeight(), constraint.clearWhenZero(),
                debugEnabled(subjectId));
    }

    private IkConstraintSnapshot emptySnapshot(String subjectId) {
        return new IkConstraintSnapshot(subjectId, Optional.empty(), Optional.empty(), 0.0D,
                false, debugEnabled(subjectId));
    }
}
