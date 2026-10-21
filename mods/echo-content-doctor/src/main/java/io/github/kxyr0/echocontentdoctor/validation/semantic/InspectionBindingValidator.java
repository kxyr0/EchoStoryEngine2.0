package io.github.kxyr0.echocontentdoctor.validation.semantic;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public final class InspectionBindingValidator {
    public List<ContentDiagnostic> validate(ContentGraph graph) {
        var owners = new HashMap<ContentResourceId, ContentResourceId>();
        var diagnostics = new ArrayList<ContentDiagnostic>();
        for (var resource : graph.snapshot().resources()) {
            if (resource.kind() != ContentResourceKind.ITEM_INSPECTION) continue;
            for (var reference : resource.references()) {
                if (reference.kind() != ContentReferenceKind.ITEM) continue;
                var previous = owners.putIfAbsent(reference.target(), resource.id());
                if (previous != null) diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR,
                        "ITEM_INSPECTION_DUPLICATE_BINDING", resource.id(), reference.span(),
                        "Item \"" + reference.target().value() + "\" is already bound by " + previous.value(),
                        List.of()));
            }
        }
        return diagnostics;
    }
}
