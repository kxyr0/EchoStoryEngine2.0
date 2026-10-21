package io.github.kxyr0.echoik.core.solver;

import io.github.kxyr0.echoik.core.model.Pose;
import java.util.List;
import java.util.Objects;

/**
 * Immutable solver output.
 */
public record IkResult(Pose pose, IkStatus status, List<String> changedBoneIds, List<String> diagnostics) {
    public IkResult {
        pose = Objects.requireNonNull(pose, "pose");
        status = Objects.requireNonNull(status, "status");
        changedBoneIds = List.copyOf(Objects.requireNonNull(changedBoneIds, "changedBoneIds"));
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }

    public boolean solved() {
        return status != IkStatus.INVALID;
    }

    public static IkResult unsolved(Pose pose, List<String> diagnostics) {
        return new IkResult(pose, IkStatus.INVALID, List.of(), diagnostics);
    }
}
