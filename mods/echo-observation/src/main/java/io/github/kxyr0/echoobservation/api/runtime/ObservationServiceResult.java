package io.github.kxyr0.echoobservation.api.runtime;

import io.github.kxyr0.echoobservation.api.diagnostic.ObservationDiagnosticCode;
import java.util.Objects;
import java.util.Optional;

public record ObservationServiceResult(boolean successful, Optional<ObservationDiagnosticCode> error) {
    public ObservationServiceResult {
        error = Objects.requireNonNull(error, "error");
    }

    public static ObservationServiceResult success() {
        return new ObservationServiceResult(true, Optional.empty());
    }

    public static ObservationServiceResult failure(ObservationDiagnosticCode code) {
        return new ObservationServiceResult(false, Optional.of(code));
    }
}
