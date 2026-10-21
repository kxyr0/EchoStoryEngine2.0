package io.github.kxyr0.echodevscenarios.compile;

import io.github.kxyr0.echoscript.api.ParseRequest;
import io.github.kxyr0.echoscript.api.source.SourceId;
import io.github.kxyr0.echoscript.language.EchoScriptLanguage;
import io.github.kxyr0.echodevscenarios.compile.binding.ScenarioBinder;
import io.github.kxyr0.echodevscenarios.compile.source.ScenarioSourceInput;
import java.util.ArrayList;
import java.util.Optional;

public final class ScenarioCompiler {
    private final ScenarioBinder binder;

    public ScenarioCompiler(ScenarioCatalog catalog) {
        binder = new ScenarioBinder(catalog, ScenarioSetupRegistry.standard());
    }

    public ScenarioCompileResult compile(ScenarioSourceInput input) {
        var parsed = EchoScriptLanguage.parser().parse(ParseRequest.ofV1(
                new SourceId(input.sourcePath()), input.sourceText()));
        var diagnostics = new ArrayList<>(parsed.diagnostics().stream().map(value -> new ScenarioDiagnostic(
                "DEV_SCENARIO_INVALID", value.message(), value.span().orElseThrow())).toList());
        if (!parsed.successful()) return new ScenarioCompileResult(Optional.empty(), Optional.empty(), diagnostics);
        ScenarioCompileResult bound = binder.bind(parsed.syntaxTree().orElseThrow().classDeclaration(), input);
        diagnostics.addAll(bound.diagnostics());
        return new ScenarioCompileResult(bound.definition(), bound.descriptor(), diagnostics);
    }
}
