package io.github.kxyr0.echoik.animationadapter;

import io.github.kxyr0.echoik.core.solver.IkResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Result of an animation-adapter IK pass.
 */
public record AnimationIkReport(boolean applied, Optional<IkResult> result, List<String> diagnostics) {
    public AnimationIkReport {
        result = Objects.requireNonNull(result, "result");
        diagnostics = List.copyOf(Objects.requireNonNull(diagnostics, "diagnostics"));
    }
}
