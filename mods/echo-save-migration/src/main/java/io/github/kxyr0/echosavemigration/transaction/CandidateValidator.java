package io.github.kxyr0.echosavemigration.transaction;

import io.github.kxyr0.echosavemigration.api.SaveSchemaVersion;
import io.github.kxyr0.echosavemigration.data.SaveSnapshot;

@FunctionalInterface
public interface CandidateValidator {
    ValidationResult validate(SaveSnapshot candidate, SaveSchemaVersion expected);

    record ValidationResult(boolean valid, String detail) {
        public static ValidationResult accepted() { return new ValidationResult(true, ""); }
        public static ValidationResult refused(String detail) { return new ValidationResult(false, detail); }
    }
}
