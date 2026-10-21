package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.provider.SceneDescriptorAdapter;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echoscene.api.diagnostic.SceneDiagnostic;
import io.github.kxyr0.echoscene.compile.SceneSymbolCatalog;
import io.github.kxyr0.echoscene.compile.pipeline.SceneCompiler;
import io.github.kxyr0.echoscene.compile.source.SceneSourceInput;
import java.nio.file.Path;
import java.util.List;

public final class SceneSourceAdapter implements SourceAdapter {
    private final SceneCompiler compiler;
    private final SceneDescriptorAdapter descriptors = new SceneDescriptorAdapter();

    public SceneSourceAdapter(SceneSymbolCatalog symbols) { compiler = new SceneCompiler(symbols); }

    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_scene/scenes/") && value.endsWith(".es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        String namespace = SourceIdentities.inferred(sourceId).namespace();
        var result = compiler.compile(new SceneSourceInput(namespace, sourceId, sourceText,
                Integer.toUnsignedLong(sourceText.hashCode()) + 1L));
        List<ContentSourceProblem> problems = result.diagnostics().stream().map(value ->
                new ContentSourceProblem(value.severity() == SceneDiagnostic.Severity.ERROR
                        ? ContentSeverity.ERROR : ContentSeverity.WARNING, value.code(),
                        SourceIdentities.inferred(sourceId), sourceId, sourceText, value.span().start().line(),
                        value.span().start().column(), value.message())).toList();
        return result.descriptor().map(value -> new AdaptedSource(List.of(descriptors.adapt(value)), problems))
                .orElseGet(() -> new AdaptedSource(List.of(), problems));
    }
}
