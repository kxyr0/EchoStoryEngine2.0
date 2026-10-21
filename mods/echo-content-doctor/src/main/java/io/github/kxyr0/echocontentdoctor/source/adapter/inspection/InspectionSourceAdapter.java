package io.github.kxyr0.echocontentdoctor.source.adapter.inspection;

import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.provider.InspectionDescriptorAdapter;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echoiteminspection.compile.InspectionCompiler;
import io.github.kxyr0.echoiteminspection.compile.InspectionSource;
import io.github.kxyr0.echoiteminspection.model.ItemInspectionDefinitionRevision;
import java.nio.file.Path;

public final class InspectionSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) { return path.toString().endsWith(".inspection.es"); }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var source = new InspectionSource(sourceId, sourceText);
        var result = new InspectionCompiler().compile(source,
                new ItemInspectionDefinitionRevision(1));
        var descriptors = result.definition().stream()
                .map(value -> InspectionDescriptorAdapter.adapt(value.descriptor())).toList();
        var problems = result.diagnostics().stream().map(value -> new ContentSourceProblem(
                value.error() ? ContentSeverity.ERROR : ContentSeverity.WARNING, value.code(),
                resourceId(source), sourceId, sourceText,
                value.span().start().line(), value.span().start().column(), value.message())).toList();
        return new AdaptedSource(descriptors, problems);
    }

    private ContentResourceId resourceId(InspectionSource source) {
        try {
            return new ContentResourceId(source.inferredId().value());
        } catch (IllegalArgumentException invalid) {
            return new ContentResourceId("echo_item_inspection:invalid");
        }
    }
}
