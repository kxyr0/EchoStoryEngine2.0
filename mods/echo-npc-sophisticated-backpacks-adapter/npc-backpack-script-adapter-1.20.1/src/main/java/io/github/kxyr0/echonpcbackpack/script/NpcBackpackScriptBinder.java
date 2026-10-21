package io.github.kxyr0.echonpcbackpack.script;

import io.github.kxyr0.echoscript.api.syntax.CompilationUnitSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.MemberAccessExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Host-neutral semantic binder for the canonical Npc backpack calls. */
public final class NpcBackpackScriptBinder {
    private static final Map<String, Integer> ARITY = Map.of(
            NpcBackpackScriptCalls.EQUIP, 2,
            NpcBackpackScriptCalls.UNEQUIP, 2,
            NpcBackpackScriptCalls.IS_WEARING, 1,
            NpcBackpackScriptCalls.HAS_TRANSITION, 1,
            NpcBackpackScriptCalls.IS_AT, 1);

    public List<NpcBackpackScriptBinding> bind(CompilationUnitSyntax unit) {
        List<NpcBackpackScriptBinding> result = new ArrayList<>();
        unit.classDeclarations().stream()
                .flatMap(type -> type.members().stream())
                .filter(MethodSyntax.class::isInstance)
                .map(MethodSyntax.class::cast)
                .flatMap(method -> method.body().statements().stream())
                .filter(ExpressionStatementSyntax.class::isInstance)
                .map(ExpressionStatementSyntax.class::cast)
                .map(ExpressionStatementSyntax::expression)
                .filter(InvocationExpressionSyntax.class::isInstance)
                .map(InvocationExpressionSyntax.class::cast)
                .forEach(call -> bindCall(call).ifPresent(result::add));
        return List.copyOf(result);
    }

    private Optional<NpcBackpackScriptBinding> bindCall(InvocationExpressionSyntax call) {
        Optional<String> name = canonicalName(call);
        if (name.isEmpty() || !ARITY.containsKey(name.orElseThrow())) return Optional.empty();
        int expected = ARITY.get(name.orElseThrow());
        if (call.arguments().size() != expected || call.arguments().stream().anyMatch(arg -> arg.name().isPresent())) {
            throw new IllegalArgumentException(name.orElseThrow() + " requires " + expected
                    + " positional arguments.");
        }
        return Optional.of(new NpcBackpackScriptBinding(name.orElseThrow(), call));
    }

    private Optional<String> canonicalName(InvocationExpressionSyntax call) {
        if (!(call.target() instanceof MemberAccessExpressionSyntax member)
                || !(member.target() instanceof NameExpressionSyntax owner)) return Optional.empty();
        return Optional.of(owner.name() + "." + member.memberName());
    }
}
