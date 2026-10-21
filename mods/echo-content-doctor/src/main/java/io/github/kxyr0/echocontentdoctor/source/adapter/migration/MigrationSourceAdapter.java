package io.github.kxyr0.echocontentdoctor.source.adapter.migration;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echosavemigration.compile.MigrationSourceInput;
import io.github.kxyr0.echosavemigration.compile.SaveMigrationCompiler;
import java.nio.file.Path;
import java.util.List;

public final class MigrationSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_save_migration/migrations/") && value.endsWith(".migration.es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        String namespace = SourceIdentities.inferred(sourceId).namespace();
        var result = new SaveMigrationCompiler().compile(new MigrationSourceInput(namespace, sourceId, sourceText,
                Integer.toUnsignedLong(sourceText.hashCode()) + 1L));
        List<ContentSourceProblem> problems = result.diagnostics().stream().map(value ->
                new ContentSourceProblem(ContentSeverity.ERROR, value.code(), SourceIdentities.inferred(sourceId),
                        sourceId, sourceText, value.span().start().line(), value.span().start().column(),
                        value.message())).toList();
        return result.definition().map(value -> {
            ContentDescriptor descriptor = ContentDescriptor.simple(new ContentResourceId(value.id().value()),
                    ContentResourceKind.MIGRATION, SourceSpans.file(sourceId, sourceText), List.of());
            return new AdaptedSource(List.of(descriptor), problems, List.of(value));
        }).orElseGet(() -> new AdaptedSource(List.of(), problems));
    }
}
