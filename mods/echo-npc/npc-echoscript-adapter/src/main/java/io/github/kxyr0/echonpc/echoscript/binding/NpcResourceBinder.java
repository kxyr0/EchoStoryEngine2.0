package io.github.kxyr0.echonpc.echoscript.binding;

import io.github.kxyr0.echonpc.core.diagnostic.NpcDiagnostic;
import io.github.kxyr0.echonpc.echoscript.NpcCompileResult;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MemberSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * NpcResource semantic binder. It reads AST data only and never executes source code.
 */
public final class NpcResourceBinder {
    private final NpcCallShape shape = new NpcCallShape();
    private final NpcInvocationReader invocationReader = new NpcInvocationReader();

    public NpcCompileResult bind(CompilationUnitSyntax unit) {
        return bind(unit, "");
    }

    public NpcCompileResult bind(CompilationUnitSyntax unit, String sourceId) {
        List<NpcDiagnostic> diagnostics = new ArrayList<>();
        ClassSyntax resource = unit.classDeclaration();
        requireNpcResource(resource, diagnostics);
        if (resource.members().stream().anyMatch(
                io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax.class::isInstance)
                || resource.members().isEmpty()) {
            return new HighLevelNpcBinder().bind(resource, sourceId, diagnostics);
        }
        Optional<MethodSyntax> build = buildMethod(resource, diagnostics);
        Optional<InvocationExpressionSyntax> npc = build.flatMap(method -> npcReturn(method, diagnostics));
        var definition = npc.flatMap(call -> invocationReader.read(call, diagnostics));
        if (diagnostics.stream().anyMatch(NpcDiagnostic::error)) {
            return new NpcCompileResult(Optional.empty(), diagnostics);
        }
        return new NpcCompileResult(definition, diagnostics);
    }

    private void requireNpcResource(ClassSyntax resource, List<NpcDiagnostic> diagnostics) {
        if (!"NpcResource".equals(resource.baseType().name().text())) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-BASE",
                    "Источник должен объявлять class ... : NpcResource.", resource.name(), resource.span()));
        }
    }

    private Optional<MethodSyntax> buildMethod(ClassSyntax resource, List<NpcDiagnostic> diagnostics) {
        List<MethodSyntax> matches = resource.members().stream()
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .filter(method -> "Build".equals(method.name()))
                .toList();
        if (matches.size() != 1) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-BUILD",
                    "NpcResource должен содержать ровно один Build method.", resource.name(), resource.span()));
            return Optional.empty();
        }
        return Optional.of(matches.get(0));
    }

    private Optional<InvocationExpressionSyntax> npcReturn(MethodSyntax build, List<NpcDiagnostic> diagnostics) {
        int npcCount = shape.countNpcCalls(build.body());
        if (npcCount > 1) {
            diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-MULTIPLE-NPC",
                    "В одном NpcResource разрешён ровно один Npc(...).", build.name(), build.span()));
        }
        for (var statement : build.body().statements()) {
            if (statement instanceof ReturnStatementSyntax returnStatement && returnStatement.value().isPresent()) {
                return readReturnedNpc(returnStatement, diagnostics);
            }
        }
        diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-NO-RETURN",
                "Build должен возвращать Npc(...).", build.name(), build.span()));
        return Optional.empty();
    }

    private Optional<InvocationExpressionSyntax> readReturnedNpc(
            ReturnStatementSyntax returnStatement,
            List<NpcDiagnostic> diagnostics
    ) {
        if (returnStatement.value().orElseThrow() instanceof InvocationExpressionSyntax call
                && shape.named(call, "Npc")) {
            return Optional.of(call);
        }
        diagnostics.add(SemanticDiagnosticFactory.error("ENPC-NPCRESOURCE-INTRINSIC",
                "Build должен возвращать intrinsic Npc(...).", "return", returnStatement.span()));
        return Optional.empty();
    }
}
