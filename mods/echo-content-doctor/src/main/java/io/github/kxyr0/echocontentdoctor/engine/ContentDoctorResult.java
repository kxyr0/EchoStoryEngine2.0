package io.github.kxyr0.echocontentdoctor.engine;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.List;

public record ContentDoctorResult(
        long revision,
        int files,
        int resources,
        int references,
        List<ContentDiagnostic> diagnostics
) {
    public ContentDoctorResult {
        diagnostics = diagnostics.stream().sorted().toList();
    }

    public long errors() {
        return diagnostics.stream().filter(value -> value.severity() == ContentSeverity.ERROR).count();
    }

    public long warnings() {
        return diagnostics.stream().filter(value -> value.severity() == ContentSeverity.WARNING).count();
    }

    public boolean successful() {
        return errors() == 0;
    }
}
