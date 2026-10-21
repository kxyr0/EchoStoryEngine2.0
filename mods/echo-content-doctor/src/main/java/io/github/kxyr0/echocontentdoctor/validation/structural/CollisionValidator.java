package io.github.kxyr0.echocontentdoctor.validation.structural;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.List;

public final class CollisionValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        graph.resources().forEach((key, values) -> {
            if (values.size() > 1) {
                for (ContentDescriptor value : values) diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR,
                        "ECHO_DOCTOR_CATALOG_COLLISION", value.id(), value.span(),
                        "Duplicate " + key.kind() + " resource id " + key.id().value(), List.of()));
            }
        });
        return diagnostics;
    }
}
