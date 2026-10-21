package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.api.source.SourcePosition;
import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.engine.ContentDoctorInput;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echosavemigration.graph.MigrationGraph;
import io.github.kxyr0.echosavemigration.graph.MigrationPlanningException;
import java.util.List;

public final class MigrationValidator {
    public List<ContentDiagnostic> validate(ContentDoctorInput input) {
        if (input.minimumSupportedSchema().equals(input.currentSchema())) return List.of();
        try {
            new MigrationGraph(input.migrations()).plan(input.minimumSupportedSchema(), input.currentSchema());
            return List.of();
        } catch (MigrationPlanningException failure) {
            SourcePosition point = new SourcePosition(0, 1, 1);
            SourceSpan span = new SourceSpan(new SourceId("migrations"), 0, 0, point, point);
            return List.of(new ContentDiagnostic(ContentSeverity.ERROR, "ECHO_DOCTOR_" + failure.code(),
                    new ContentResourceId("echo:migrations"), span, failure.getMessage(), List.of()));
        }
    }
}
