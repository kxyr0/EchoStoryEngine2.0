package io.github.kxyr0.echovisibility.core.report;

import io.github.kxyr0.echovisibility.api.diagnostic.VisibilityDiagnostic;
import java.util.Objects;
import java.util.Optional;

public record VisibilityReportValidation(Status status, Optional<VisibilityDiagnostic> diagnostic) {
    public VisibilityReportValidation {
        status = Objects.requireNonNull(status, "status");
        diagnostic = Objects.requireNonNull(diagnostic, "diagnostic");
    }

    public static VisibilityReportValidation accepted() {
        return new VisibilityReportValidation(Status.ACCEPTED, Optional.empty());
    }

    public enum Status {
        ACCEPTED,
        GENERATION_MISMATCH,
        REVISION_MISMATCH,
        CONNECTION_MISMATCH,
        SEQUENCE_INVALID,
        CAMERA_INVALID,
        TRANSITION_INVALID,
        CONCEALMENT_INVALID
    }
}
