package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.api.diagnostic.DialogueDiagnostic;
import io.github.kxyr0.echodialogue.core.model.command.CompiledDialogueCommand;
import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echodialogue.echoscript.diagnostic.DialogueSemanticDiagnostics;
import io.github.kxyr0.echoscript.api.syntax.expression.ArgumentSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.NameExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TimeCommandReader {
    private static final Set<String> COMMANDS = Set.of(
            "Time.Set", "Time.Add", "Time.Pause", "Time.Resume", "Time.WaitUntil",
            "Time.Is", "Time.IsBefore", "Time.IsAfter", "Time.IsBetween");

    public Optional<CompiledDialogueCommand> read(
            InvocationExpressionSyntax call, int position, String command,
            List<DialogueDiagnostic> diagnostics) {
        Map<String, String> values = new LinkedHashMap<>();
        List<ArgumentSyntax> positional = call.arguments().stream().filter(value -> value.name().isEmpty()).toList();
        boolean valid = COMMANDS.contains(command) && namedValid(call, command);
        valid &= switch (command) {
            case "Time.Set" -> set(positional, call, values);
            case "Time.Add" -> add(positional, call, values);
            case "Time.Pause", "Time.Resume" -> positional.isEmpty();
            case "Time.WaitUntil", "Time.Is", "Time.IsBefore", "Time.IsAfter" ->
                    oneTime(positional, values);
            case "Time.IsBetween" -> between(positional, values);
            default -> false;
        };
        if (!valid) {
            diagnostics.add(DialogueSemanticDiagnostics.semantic("EDLG-SEM-076",
                    "Invalid " + command + " arguments.", call));
            return Optional.empty();
        }
        return Optional.of(new CompiledDialogueCommand(type(command), position, values));
    }

    private boolean set(List<ArgumentSyntax> positional, InvocationExpressionSyntax call,
            Map<String, String> values) {
        if (positional.size() == 1) {
            ArgumentValues.simple(positional.get(0).value()).ifPresent(value -> values.put("time", value));
        } else if (positional.size() == 2) {
            unary(positional.get(0), "Day").ifPresent(value -> values.put("day", value));
            ArgumentValues.simple(positional.get(1).value()).ifPresent(value -> values.put("time", value));
        } else {
            return false;
        }
        values.put("crossingPolicy", named(call, "crossed").orElse("SKIP_CROSSED"));
        return values.containsKey("time") && (positional.size() == 1 || values.containsKey("day"));
    }

    private boolean add(List<ArgumentSyntax> positional, InvocationExpressionSyntax call,
            Map<String, String> values) {
        if (positional.size() != 1) {
            return false;
        }
        Optional<Unary> duration = unary(positional.get(0), Set.of("Minutes", "Hours"));
        duration.ifPresent(value -> {
            values.put("durationUnit", value.name());
            values.put("durationValue", value.value());
        });
        values.put("crossingPolicy", named(call, "crossed").orElse("FIRE_CROSSED"));
        return duration.isPresent();
    }

    private boolean oneTime(List<ArgumentSyntax> positional, Map<String, String> values) {
        if (positional.size() != 1) {
            return false;
        }
        ArgumentValues.simple(positional.get(0).value()).ifPresent(value -> values.put("time", value));
        return values.containsKey("time");
    }

    private boolean between(List<ArgumentSyntax> positional, Map<String, String> values) {
        if (positional.size() != 2) {
            return false;
        }
        ArgumentValues.simple(positional.get(0).value()).ifPresent(value -> values.put("start", value));
        ArgumentValues.simple(positional.get(1).value()).ifPresent(value -> values.put("end", value));
        return values.size() == 2;
    }

    private boolean namedValid(InvocationExpressionSyntax call, String command) {
        Set<String> allowed = command.equals("Time.Set") || command.equals("Time.Add")
                ? Set.of("crossed") : Set.of();
        return call.arguments().stream().flatMap(value -> value.name().stream()).allMatch(allowed::contains);
    }

    private Optional<String> named(InvocationExpressionSyntax call, String name) {
        return call.arguments().stream().filter(value -> value.name().filter(name::equals).isPresent())
                .findFirst().flatMap(value -> ArgumentValues.simple(value.value()));
    }

    private Optional<String> unary(ArgumentSyntax argument, String name) {
        return unary(argument, Set.of(name)).map(Unary::value);
    }

    private Optional<Unary> unary(ArgumentSyntax argument, Set<String> names) {
        if (argument.value() instanceof InvocationExpressionSyntax invocation
                && invocation.target() instanceof NameExpressionSyntax name
                && names.contains(name.name()) && invocation.arguments().size() == 1
                && invocation.arguments().get(0).name().isEmpty()) {
            return ArgumentValues.simple(invocation.arguments().get(0).value())
                    .map(value -> new Unary(name.name(), value));
        }
        return Optional.empty();
    }

    private String type(String command) {
        return "echo_time:" + command.substring("Time.".length())
                .replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(java.util.Locale.ROOT);
    }

    private record Unary(String name, String value) { }
}
