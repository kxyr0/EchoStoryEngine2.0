package io.github.kxyr0.echodialogue.echoscript.binding.backpack;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.IntegerLiteralExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.ObjectCreationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.StringLiteralExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class BackpackDialogueCommandReader {
    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call,
            boolean equip,
            ReadContext context) {
        Optional<Map<String, String>> arguments = arguments(
                call, context.actorResolver(), context.blockPositionResolver());
        if (arguments.isEmpty()) {
            context.diagnostics().add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-072",
                    "Backpack command requires npc and Position/BlockPosition or npc, x, y, z arguments.", call));
            return Optional.empty();
        }
        String type = equip ? "echo_npc_sophisticated_backpacks:equip"
                : "echo_npc_sophisticated_backpacks:unequip";
        return Optional.of(new CompiledDialogueCommand(type, context.position(), arguments.orElseThrow()));
    }

    private Optional<Map<String, String>> arguments(InvocationExpressionSyntax call,
            Function<String, String> actorResolver,
            Function<String, Optional<Map<String, String>>> blockPositionResolver) {
        if (call.arguments().size() < 2) {
            return Optional.empty();
        }
        Optional<String> actor = simple(call.arguments().get(0).value());
        Optional<Map<String, String>> position = position(call, blockPositionResolver);
        if (actor.isEmpty() || position.isEmpty()) {
            return Optional.empty();
        }
        Map<String, String> values = new LinkedHashMap<>(position.orElseThrow());
        values.put("npc", actorResolver.apply(actor.orElseThrow()));
        values.put("dimension", named(call, "dimension").orElse("minecraft:overworld"));
        return Optional.of(Map.copyOf(values));
    }

    private Optional<Map<String, String>> position(InvocationExpressionSyntax call,
            Function<String, Optional<Map<String, String>>> resolver) {
        Optional<Map<String, String>> coordinates = coordinatePosition(call);
        if (coordinates.isPresent()) {
            return coordinates;
        }
        ExpressionSyntax expression = call.arguments().get(1).value();
        Optional<String> reference = simple(expression);
        if (reference.isPresent()) {
            Optional<Map<String, String>> resolved = resolver.apply(reference.orElseThrow());
            if (resolved.isPresent()) {
                return resolved;
            }
        }
        return inlinePosition(expression);
    }

    private Optional<Map<String, String>> coordinatePosition(InvocationExpressionSyntax call) {
        if (call.arguments().size() < 4) {
            return Optional.empty();
        }
        Optional<String> x = simple(call.arguments().get(1).value());
        Optional<String> y = simple(call.arguments().get(2).value());
        Optional<String> z = simple(call.arguments().get(3).value());
        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(Map.of("x", x.orElseThrow(), "y", y.orElseThrow(), "z", z.orElseThrow()))
                : Optional.empty();
    }

    private Optional<Map<String, String>> inlinePosition(ExpressionSyntax expression) {
        Optional<List<ArgumentSyntax>> syntax = positionArguments(expression);
        if (syntax.isEmpty()) {
            return Optional.empty();
        }
        List<ArgumentSyntax> arguments = syntax.orElseThrow();
        if (arguments.size() != 3 || arguments.stream().anyMatch(argument -> argument.name().isPresent())) {
            return Optional.empty();
        }
        Optional<String> x = simple(arguments.get(0).value());
        Optional<String> y = simple(arguments.get(1).value());
        Optional<String> z = simple(arguments.get(2).value());
        return x.isPresent() && y.isPresent() && z.isPresent()
                ? Optional.of(Map.of("x", x.orElseThrow(), "y", y.orElseThrow(), "z", z.orElseThrow()))
                : Optional.empty();
    }

    private Optional<List<ArgumentSyntax>> positionArguments(ExpressionSyntax expression) {
        if (expression instanceof InvocationExpressionSyntax invocation
                && invocation.target() instanceof NameExpressionSyntax name && name.name().equals("Position")) {
            return Optional.of(invocation.arguments());
        }
        if (expression instanceof ObjectCreationExpressionSyntax creation
                && creation.type().name().text().equals("BlockPosition")) {
            return Optional.of(creation.arguments());
        }
        return Optional.empty();
    }

    private Optional<String> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(argument -> argument.name().filter(name::equals).isPresent())
                .map(ArgumentSyntax::value).flatMap(value -> simple(value).stream()).findFirst();
    }

    private Optional<String> simple(ExpressionSyntax expression) {
        if (expression instanceof StringLiteralExpressionSyntax literal) {
            return Optional.of(literal.value());
        }
        if (expression instanceof IntegerLiteralExpressionSyntax literal) {
            return Optional.of(Integer.toString(literal.value()));
        }
        if (expression instanceof NameExpressionSyntax name) {
            return Optional.of(name.name());
        }
        return Optional.empty();
    }

    public record ReadContext(
            int position,
            Function<String, String> actorResolver,
            Function<String, Optional<Map<String, String>>> blockPositionResolver,
            List<DialogueDiagnostic> diagnostics) {
    }
}
