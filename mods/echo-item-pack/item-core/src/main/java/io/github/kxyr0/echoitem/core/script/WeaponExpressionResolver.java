package io.github.kxyr0.echoitem.core.script;

import io.github.kxyr0.echoitem.api.diagnostic.ItemDefinitionDiagnostic;
import io.github.kxyr0.echoitem.api.diagnostic.ItemDiagnosticCodes;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.FieldSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.MethodSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ArrayLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ParenthesizedExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.LocalDeclarationStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ReturnStatementSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.StatementSyntax;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class WeaponExpressionResolver {
    private static final String WEAPON_INTRINSIC = "Weapon";
    private final Map<String, ExpressionSyntax> fields = new HashMap<>();
    private final Map<String, MethodSyntax> methods = new HashMap<>();
    private final String sourcePath;
    private final List<ItemDefinitionDiagnostic> diagnostics;

    WeaponExpressionResolver(ClassSyntax resource, String sourcePath, List<ItemDefinitionDiagnostic> diagnostics) {
        this.sourcePath = sourcePath;
        this.diagnostics = diagnostics;
        resource.members().forEach(member -> {
            if (member instanceof FieldSyntax field && field.initializer().isPresent()) {
                fields.put(field.name(), field.initializer().orElseThrow());
            }
            if (member instanceof MethodSyntax method) {
                methods.put(method.name(), method);
            }
        });
    }

    Optional<ExpressionSyntax> resolveMethodReturn(MethodSyntax method) {
        return resolveMethodReturn(method, new LinkedHashSet<>());
    }

    ExpressionSyntax resolve(ExpressionSyntax expression, Map<String, ExpressionSyntax> locals, Set<String> stack) {
        if (expression instanceof NameExpressionSyntax name) {
            return resolveName(name, locals, stack).orElse(expression);
        }
        if (expression instanceof InvocationExpressionSyntax invocation) {
            return resolveInvocation(invocation, locals, stack);
        }
        if (expression instanceof ArrayLiteralExpressionSyntax array) {
            return new ArrayLiteralExpressionSyntax(array.elements().stream()
                    .map(element -> resolve(element, locals, stack))
                    .toList(), array.span());
        }
        if (expression instanceof ParenthesizedExpressionSyntax parenthesized) {
            return resolve(parenthesized.expression(), locals, stack);
        }
        return expression;
    }

    private ExpressionSyntax resolveInvocation(InvocationExpressionSyntax invocation,
                                               Map<String, ExpressionSyntax> locals,
                                               Set<String> stack) {
        Optional<ExpressionSyntax> methodValue = resolveMethodInvocation(invocation, stack);
        if (methodValue.isPresent()) {
            return methodValue.orElseThrow();
        }
        List<ArgumentSyntax> resolvedArguments = invocation.arguments().stream()
                .map(argument -> new ArgumentSyntax(argument.name(),
                        resolve(argument.value(), locals, stack), argument.span()))
                .toList();
        return new InvocationExpressionSyntax(invocation.target(), resolvedArguments, invocation.span());
    }

    private Optional<ExpressionSyntax> resolveName(NameExpressionSyntax name, Map<String, ExpressionSyntax> locals,
                                                   Set<String> stack) {
        if (locals.containsKey(name.name())) {
            return resolveSymbol("local:" + name.name(), locals.get(name.name()), locals, stack);
        }
        if (fields.containsKey(name.name())) {
            return resolveSymbol("field:" + name.name(), fields.get(name.name()), Map.of(), stack);
        }
        return Optional.empty();
    }

    private Optional<ExpressionSyntax> resolveMethodInvocation(InvocationExpressionSyntax invocation,
                                                               Set<String> stack) {
        if (!(invocation.target() instanceof NameExpressionSyntax target)
                || WEAPON_INTRINSIC.equals(target.name())
                || !invocation.arguments().isEmpty()
                || !methods.containsKey(target.name())) {
            return Optional.empty();
        }
        MethodSyntax method = methods.get(target.name());
        if (!method.parameters().isEmpty()) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "Weapon helper methods used as values must have no parameters: " + target.name(), sourcePath));
            return Optional.empty();
        }
        String key = "method:" + target.name();
        if (!stack.add(key)) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "Cyclic weapon helper method reference: " + target.name(), sourcePath));
            return Optional.empty();
        }
        Optional<ExpressionSyntax> result = resolveMethodReturn(method, stack);
        stack.remove(key);
        return result;
    }

    private Optional<ExpressionSyntax> resolveMethodReturn(MethodSyntax method, Set<String> stack) {
        return returnExpression(method).map(expression -> resolve(expression, localBindings(method), stack));
    }

    private Optional<ExpressionSyntax> resolveSymbol(String key, ExpressionSyntax expression,
                                                     Map<String, ExpressionSyntax> locals, Set<String> stack) {
        if (!stack.add(key)) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "Cyclic weapon value reference: " + key.substring(key.indexOf(':') + 1), sourcePath));
            return Optional.empty();
        }
        ExpressionSyntax resolved = resolve(expression, locals, stack);
        stack.remove(key);
        return Optional.of(resolved);
    }

    private Optional<ExpressionSyntax> returnExpression(MethodSyntax method) {
        List<ReturnStatementSyntax> returns = method.body().statements().stream()
                .filter(ReturnStatementSyntax.class::isInstance)
                .map(ReturnStatementSyntax.class::cast)
                .filter(statement -> statement.value().isPresent())
                .toList();
        if (returns.size() != 1) {
            diagnostics.add(ItemDefinitionDiagnostic.error(ItemDiagnosticCodes.ITEM_SCRIPT_BIND_ERROR,
                    "Weapon helper methods used as values must return exactly one expression: " + method.name(),
                    sourcePath));
            return Optional.empty();
        }
        return returns.get(0).value();
    }

    private Map<String, ExpressionSyntax> localBindings(MethodSyntax method) {
        Map<String, ExpressionSyntax> bindings = new LinkedHashMap<>();
        for (StatementSyntax statement : method.body().statements()) {
            if (statement instanceof ReturnStatementSyntax) {
                return bindings;
            }
            if (statement instanceof LocalDeclarationStatementSyntax local && local.initializer().isPresent()) {
                bindings.put(local.name(), local.initializer().orElseThrow());
            }
        }
        return bindings;
    }
}
