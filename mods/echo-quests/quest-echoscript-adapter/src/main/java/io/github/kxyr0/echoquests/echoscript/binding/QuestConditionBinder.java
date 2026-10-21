package io.github.kxyr0.echoquests.echoscript.binding;

import io.github.kxyr0.echoquests.api.condition.QuestCondition;
import io.github.kxyr0.echoscript.api.syntax.expression.ExpressionSyntax;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

final class QuestConditionBinder {
    private static final Pattern RESOURCE_ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private final QuestCallReader calls;

    QuestConditionBinder(QuestCallReader calls) { this.calls = calls; }

    QuestCondition bind(ExpressionSyntax expression) {
        if (!(expression instanceof InvocationExpressionSyntax call)) throw failure("when must be a condition call.");
        return switch (calls.name(call)) {
            case "All" -> group(call, "all", 1);
            case "Any" -> group(call, "any", 1);
            case "Not" -> group(call, "not", 1, 1);
            case "Player.InArea" -> area(call);
            case "Player.HasItem" -> item(call);
            default -> throw failure("Unknown quest condition: " + calls.name(call));
        };
    }

    private QuestCondition group(InvocationExpressionSyntax call, String type, int minimum) {
        return group(call, type, minimum, Integer.MAX_VALUE);
    }

    private QuestCondition group(InvocationExpressionSyntax call, String type, int minimum, int maximum) {
        rejectNamed(call, Set.of());
        List<ExpressionSyntax> values = calls.positionals(call);
        if (values.size() < minimum || values.size() > maximum) throw failure(type + " has invalid child count.");
        return new QuestCondition(type, Map.of(), values.stream().map(this::bind).toList());
    }

    private QuestCondition area(InvocationExpressionSyntax call) {
        requirePositionals(call, "Player.InArea", 1);
        rejectNamed(call, Set.of("x", "y", "z", "radius"));
        Map<String, String> values = new LinkedHashMap<>();
        String dimension = calls.string(call, 0)
                .orElseThrow(() -> failure("Player.InArea dimension must be a string."));
        if (!RESOURCE_ID.matcher(dimension).matches()) {
            throw failure("Player.InArea has invalid dimension ID: " + dimension);
        }
        values.put("dimension", dimension);
        for (String name : List.of("x", "y", "z", "radius")) {
            values.put(name, calls.namedScalar(call, name)
                    .orElseThrow(() -> failure("Player.InArea requires numeric " + name + ".")));
            number(values.get(name), "Player.InArea " + name);
        }
        if (Double.parseDouble(values.get("radius")) <= 0.0D) throw failure("Player.InArea radius must be positive.");
        return QuestCondition.leaf("echo_quests:player_in_area", values);
    }

    private QuestCondition item(InvocationExpressionSyntax call) {
        requirePositionals(call, "Player.HasItem", 1);
        rejectNamed(call, Set.of("count"));
        String item = calls.string(call, 0).orElseThrow(() -> failure("Player.HasItem ID must be a string."));
        if (!RESOURCE_ID.matcher(item).matches()) throw failure("Player.HasItem has invalid item ID: " + item);
        String count = calls.namedScalar(call, "count").orElse("1");
        int required;
        try {
            required = Integer.parseInt(count);
        } catch (NumberFormatException exception) {
            throw failure("Player.HasItem count must be an integer.");
        }
        if (required < 1) throw failure("Player.HasItem count must be positive.");
        return QuestCondition.leaf("echo_quests:player_has_item", Map.of("item", item, "count", count));
    }

    private void requirePositionals(InvocationExpressionSyntax call, String name, int count) {
        if (calls.positionalCount(call) != count) throw failure(name + " requires " + count + " positional argument.");
    }

    private void rejectNamed(InvocationExpressionSyntax call, Set<String> allowed) {
        calls.namedArguments(call).stream().filter(name -> !allowed.contains(name)).findFirst()
                .ifPresent(name -> { throw failure("Unknown condition argument: " + name); });
    }

    private void number(String value, String name) {
        try {
            double parsed = Double.parseDouble(value);
            if (!Double.isFinite(parsed)) throw failure(name + " must be finite.");
        } catch (NumberFormatException exception) {
            throw failure(name + " must be numeric.");
        }
    }

    private IllegalArgumentException failure(String message) { return new IllegalArgumentException(message); }
}
