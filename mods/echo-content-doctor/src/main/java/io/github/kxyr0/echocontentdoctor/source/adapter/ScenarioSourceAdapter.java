package io.github.kxyr0.echocontentdoctor.source.adapter;

import io.github.kxyr0.echocontentdoctor.model.ContentSeverity;
import io.github.kxyr0.echocontentdoctor.provider.ScenarioDescriptorAdapter;
import io.github.kxyr0.echocontentdoctor.source.ContentSourceProblem;
import io.github.kxyr0.echocontentdoctor.source.SourceAdapter;
import io.github.kxyr0.echocontentdoctor.source.SourceIdentities;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCatalog;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCompiler;
import io.github.kxyr0.echodevscenarios.compile.source.ScenarioSourceInput;
import java.nio.file.Path;
import java.util.List;

public final class ScenarioSourceAdapter implements SourceAdapter {
    private final ScenarioCompiler compiler;
    private final ScenarioDescriptorAdapter descriptors = new ScenarioDescriptorAdapter();

    public ScenarioSourceAdapter(ScenarioCatalog catalog) { compiler = new ScenarioCompiler(catalog); }

    @Override
    public boolean accepts(Path path) {
        String value = path.toString().replace('\\', '/');
        return value.contains("/echo_dev_scenarios/dev/scenarios/") && value.endsWith(".es");
    }

    @Override
    public AdaptedSource adapt(String sourceId, String sourceText) {
        var result = compiler.compile(new ScenarioSourceInput(sourceId, sourceText));
        List<ContentSourceProblem> problems = result.diagnostics().stream().map(value ->
                new ContentSourceProblem(ContentSeverity.ERROR, value.code(), SourceIdentities.inferred(sourceId),
                        sourceId, sourceText, value.span().start().line(), value.span().start().column(),
                        value.message())).toList();
        if (result.definition().isEmpty() || result.descriptor().isEmpty()) {
            return new AdaptedSource(List.of(), problems);
        }
        return new AdaptedSource(List.of(descriptors.adapt(result.definition().orElseThrow(),
                result.descriptor().orElseThrow())), problems);
    }
}
