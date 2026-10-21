package io.github.kxyr0.echodevscenarios.compile.binding;

import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import io.github.kxyr0.echodevscenarios.api.ScenarioId;
import io.github.kxyr0.echodevscenarios.api.ScenarioStepId;
import io.github.kxyr0.echodevscenarios.api.ScenarioTarget;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCatalog;
import io.github.kxyr0.echodevscenarios.compile.ScenarioCompileResult;
import io.github.kxyr0.echodevscenarios.compile.ScenarioDiagnostic;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSetupRegistry;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbol;
import io.github.kxyr0.echodevscenarios.compile.ScenarioSymbolKind;
import io.github.kxyr0.echodevscenarios.compile.source.ScenarioSourceInput;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDefinition;
import io.github.kxyr0.echodevscenarios.definition.ScenarioDescriptor;
import io.github.kxyr0.echodevscenarios.definition.ScenarioSetupStep;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ScenarioBinder {
    private final ScenarioCatalog catalog;
    private final ScenarioSetupRegistry registry;
    private final ScenarioCallReader reader = new ScenarioCallReader();
    private final ScenarioIdInference ids = new ScenarioIdInference();

    public ScenarioBinder(ScenarioCatalog catalog, ScenarioSetupRegistry registry) {
        this.catalog = catalog;
        this.registry = registry;
    }

    public ScenarioCompileResult bind(ClassSyntax resource, ScenarioSourceInput input) {
        List<ScenarioDiagnostic> diagnostics = new ArrayList<>();
        if (!resource.baseType().name().text().equals("DevScenarioResource")) {
            diagnostics.add(error("DEV_SCENARIO_INVALID", "Expected DevScenarioResource.", resource.span()));
            return failed(diagnostics);
        }
        List<ClassSectionSyntax> given = sections(resource, "Given");
        List<ClassCallSyntax> launch = calls(resource, "Launch");
        if (given.size() > 1 || launch.size() != 1) {
            diagnostics.add(error("DEV_SCENARIO_INVALID", "Scenario requires one Launch and at most one Given.",
                    resource.span()));
            return failed(diagnostics);
        }
        Optional<ScenarioTarget> target = target(launch.get(0), diagnostics);
        List<String> references = new ArrayList<>();
        List<ScenarioSetupStep> setup = given.isEmpty() ? List.of()
                : setup(given.get(0), references, diagnostics);
        if (!diagnostics.isEmpty() || target.isEmpty()) return failed(diagnostics);
        ScenarioId id = ids.id(input.sourcePath());
        String title = title(resource).orElseGet(() -> ids.title(resource.name()));
        ScenarioDefinition definition = new ScenarioDefinition(id, title, ids.groups(id), setup,
                target.orElseThrow(), resource.span());
        ScenarioDescriptor descriptor = new ScenarioDescriptor(id, title, ids.groups(id), target.orElseThrow(),
                references, resource.span());
        return new ScenarioCompileResult(Optional.of(definition), Optional.of(descriptor), diagnostics);
    }

    private List<ScenarioSetupStep> setup(ClassSectionSyntax given, List<String> references,
            List<ScenarioDiagnostic> diagnostics) {
        List<ScenarioSetupStep> steps = new ArrayList<>();
        for (int index = 0; index < given.body().statements().size(); index++) {
            var statement = given.body().statements().get(index);
            if (!(statement instanceof ExpressionStatementSyntax expression)) {
                diagnostics.add(error("DEV_SCENARIO_INVALID", "Given contains a non-command statement.", statement.span()));
                continue;
            }
            Optional<ScenarioCallReader.Call> call = reader.read(expression.expression());
            if (call.isPresent()) step(call.orElseThrow(), index, references, diagnostics).ifPresent(steps::add);
            else diagnostics.add(error("DEV_SCENARIO_INVALID", "Given command must target an Echo service.",
                    expression.span()));
        }
        return List.copyOf(steps);
    }

    private Optional<ScenarioSetupStep> step(ScenarioCallReader.Call call, int index, List<String> references,
            List<ScenarioDiagnostic> diagnostics) {
        Optional<ScenarioSetupRegistry.Rule> rule = registry.find(call.service(), call.operation());
        if (rule.isEmpty() || rule.orElseThrow().arguments().size() != call.arguments().size()) {
            diagnostics.add(error("DEV_SCENARIO_INVALID", "Unsupported Given command " + call.service() + "."
                    + call.operation(), call.span()));
            return Optional.empty();
        }
        List<String> arguments = new ArrayList<>();
        for (int position = 0; position < call.arguments().size(); position++) {
            Optional<String> value = argument(call.arguments().get(position),
                    rule.orElseThrow().arguments().get(position), references, diagnostics);
            if (value.isEmpty()) return Optional.empty();
            arguments.add(value.orElseThrow());
        }
        return Optional.of(new ScenarioSetupStep(new ScenarioStepId(index), call.service(), call.operation(),
                arguments, rule.orElseThrow().blocking(), call.span()));
    }

    private Optional<String> argument(ScenarioCallReader.Value value, ScenarioSetupRegistry.Argument argument,
            List<String> references, List<ScenarioDiagnostic> diagnostics) {
        if (argument == ScenarioSetupRegistry.Argument.VALUE) return Optional.of(value.text());
        ScenarioSymbolKind kind = ScenarioSymbolKind.valueOf(argument.name());
        List<ScenarioSymbol> matches = value.literal() ? catalog.resolveResource(value.text(), kind)
                : catalog.resolve(value.text(), kind);
        if (matches.size() != 1) {
            diagnostics.add(error("DEV_SCENARIO_INVALID", argument + " '" + value.text() + "' resolved to "
                    + matches.size() + " candidates.", value.span()));
            return Optional.empty();
        }
        references.add(matches.get(0).resourceId());
        return Optional.of(matches.get(0).resourceId());
    }

    private Optional<ScenarioTarget> target(ClassCallSyntax syntax, List<ScenarioDiagnostic> diagnostics) {
        String id = reader.classCall(syntax.invocation());
        List<ScenarioTarget> targets = catalog.targets(id);
        if (targets.size() != 1) {
            diagnostics.add(error("DEV_SCENARIO_INVALID", "Launch target '" + id + "' resolved to "
                    + targets.size() + " candidates.", syntax.span()));
            return Optional.empty();
        }
        return Optional.of(targets.get(0));
    }

    private Optional<String> title(ClassSyntax resource) {
        return calls(resource, "Title").stream().findFirst().map(call -> reader.classCall(call.invocation()));
    }

    private List<ClassCallSyntax> calls(ClassSyntax resource, String name) {
        return resource.members().stream().filter(ClassCallSyntax.class::isInstance).map(ClassCallSyntax.class::cast)
                .filter(value -> invocationName(value.invocation()).equals(name)).toList();
    }

    private List<ClassSectionSyntax> sections(ClassSyntax resource, String name) {
        return resource.members().stream().filter(ClassSectionSyntax.class::isInstance)
                .map(ClassSectionSyntax.class::cast).filter(value -> invocationName(value.invocation()).equals(name))
                .toList();
    }

    private String invocationName(InvocationExpressionSyntax invocation) {
        return invocation.target() instanceof NameExpressionSyntax name ? name.name() : "";
    }

    private ScenarioCompileResult failed(List<ScenarioDiagnostic> diagnostics) {
        return new ScenarioCompileResult(Optional.empty(), Optional.empty(), diagnostics);
    }

    private ScenarioDiagnostic error(String code, String message,
            io.github.kxyr0.echoscript.api.source.SourceSpan span) {
        return new ScenarioDiagnostic(code, message, span);
    }
}
