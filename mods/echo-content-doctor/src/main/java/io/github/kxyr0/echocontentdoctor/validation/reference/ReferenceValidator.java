package io.github.kxyr0.echocontentdoctor.validation.reference;

import io.github.kxyr0.echocontentdoctor.diagnostic.ContentDiagnostic;
import io.github.kxyr0.echocontentdoctor.graph.ContentGraph;
import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentReference;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ReferenceValidator {
    private final EditDistance distance = new EditDistance();

    public List<ContentDiagnostic> validate(ContentGraph graph) {
        List<ContentDiagnostic> diagnostics = new ArrayList<>();
        for (ContentDescriptor source : graph.snapshot().resources()) {
            for (ContentReference reference : source.references()) {
                if (graph.resolve(reference.kind().targetKind(), reference.target()).isEmpty()) {
                    String code = reference.kind() == io.github.kxyr0.echocontentdoctor.model.ContentReferenceKind.AUDIO
                            ? "AUDIO_ASSET_NOT_FOUND" : "ECHO_DOCTOR_REFERENCE_NOT_FOUND";
                    diagnostics.add(new ContentDiagnostic(ContentSeverity.ERROR, code,
                            source.id(), reference.span(), reference.kind() + " \"" + reference.target().value()
                            + "\" not found.", suggestions(graph, reference)));
                }
            }
        }
        return diagnostics;
    }

    public List<ContentResourceId> suggestions(ContentGraph graph, ContentReference reference) {
        String expected = reference.target().value();
        int threshold = Math.max(3, expected.length() / 3);
        Comparator<ContentResourceId> order = Comparator
                .comparing((ContentResourceId id) -> !id.namespace().equals(reference.target().namespace()))
                .thenComparingInt(id -> distance.between(expected, id.value())).thenComparing(ContentResourceId::value);
        return graph.snapshot().resources().stream()
                .filter(value -> value.kind() == reference.kind().targetKind()).map(ContentDescriptor::id).distinct()
                .filter(id -> distance.between(expected, id.value()) <= threshold).sorted(order).limit(3).toList();
    }
}
