package io.github.kxyr0.echocontentdoctor.source.adapter.item;

import io.github.kxyr0.echocontentdoctor.model.ContentDescriptor;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceId;
import io.github.kxyr0.echocontentdoctor.model.ContentResourceKind;
import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echocontentdoctor.source.SourceSpans;
import io.github.kxyr0.echoitem.api.definition.ItemDefinitionRevision;
import io.github.kxyr0.echoitem.core.script.EchoScriptWeaponCompiler;
import java.nio.file.Path;
import java.util.List;

/** Reuses the real Item Pack semantic compiler rather than inferring item IDs from source text. */
public final class ItemDefinitionSourceAdapter implements SourceAdapter {
    @Override
    public boolean accepts(Path path) { return path.toString().endsWith(".weapon.es"); }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var result = new EchoScriptWeaponCompiler().compile(sourceId, sourceText, new ItemDefinitionRevision(1));
        var descriptors = result.definition().stream().map(value -> ContentDescriptor.simple(
                new ContentResourceId(value.id().value()), ContentResourceKind.ITEM,
                SourceSpans.file(sourceId, sourceText), List.of())).toList();
        var problems = result.diagnostics().stream().map(value -> new ContentSourceProblem(
                value.error() ? ContentSeverity.ERROR : ContentSeverity.WARNING, value.code(),
                SourceIdentities.inferred(sourceId), sourceId, sourceText, 1, 1, value.message())).toList();
        return new AdaptedSource(descriptors, problems);
    }
}
