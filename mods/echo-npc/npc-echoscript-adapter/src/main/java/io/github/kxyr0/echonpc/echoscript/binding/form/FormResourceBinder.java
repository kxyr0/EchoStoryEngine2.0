package io.github.kxyr0.echonpc.echoscript.binding.form;

import io.github.kxyr0.echonpc.api.form.NpcFormRevision;
import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.NpcFormCompileResult;
import io.github.kxyr0.echonpc.echoscript.binding.SemanticDiagnosticFactory;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NpcFormResource semantic binder. It reads AST data only and never executes source code.
 */
public final class FormResourceBinder {
    private final FormCallShape shape = new FormCallShape();
    private final FormInvocationReader invocationReader = new FormInvocationReader();

    public NpcFormCompileResult bind(String sourceId, CompilationUnitSyntax unit, NpcFormRevision revision) {
        List<NpcDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax resource = unit.classDeclaration();
        requireFormResource(resource, diagnostics);
        Optional<MethodSyntax> build = buildMethod(resource, diagnostics);
        Optional<InvocationExpressionSyntax> form = build.flatMap(method -> formReturn(method, diagnostics));
        var definition = form.flatMap(call -> invocationReader.read(sourceId, call, revision, diagnostics));
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            return new NpcFormCompileResult(Optional.empty(), diagnostics);
        }
        return new NpcFormCompileResult(definition, diagnostics);
    }

    private void requireFormResource(ClassSyntax resource, List<NpcDiagnostic> diagnostics) {
        if (!"NpcFormResource".equals(resource.baseType().name().text())) {
            diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_RESOURCE_BASE",
                    "Источник должен объявлять class ... : NpcFormResource.", resource.name(), resource.span()));
        }
    }

    private Optional<MethodSyntax> buildMethod(ClassSyntax resource, List<NpcDiagnostic> diagnostics) {
        List<MethodSyntax> matches = resource.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> "Build".equals(method.name()))
                .toList();
        if (matches.size() != 1) {
            diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_RESOURCE_BUILD",
                    "NpcFormResource должен содержать ровно один Build method.", resource.name(), resource.span()));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private Optional<InvocationExpressionSyntax> formReturn(MethodSyntax build, List<NpcDiagnostic> diagnostics) {
        if (shape.countFormCalls(build.body()) > 1) {
            diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_RESOURCE_MULTIPLE_FORM",
                    "В одном NpcFormResource разрешен ровно один Form(...).", build.name(), build.span()));
        }
        for (var statement : build.body().statements()) {
            if (statement instanceof ReturnStatementSyntax returnStatement && returnStatement.value().isPresent()) {
                return readReturnedForm(returnStatement, diagnostics);
            }
        }
        diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_RESOURCE_NO_RETURN",
                "Build должен возвращать Form(...).", build.name(), build.span()));
        return Optional.empty();
    }

    private Optional<InvocationExpressionSyntax> readReturnedForm(ReturnStatementSyntax returnStatement,
            List<NpcDiagnostic> diagnostics) {
        if (returnStatement.value().orElseThrow() instanceof InvocationExpressionSyntax call
                && shape.named(call, "Form")) {
            return Optional.of(call);
        }
        diagnostics.add(SemanticDiagnosticFactory.error("NPC_FORM_RESOURCE_INTRINSIC",
                "Build должен возвращать intrinsic Form(...).", "return", returnStatement.span()));
        return Optional.empty();
    }
}
