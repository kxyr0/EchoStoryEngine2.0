package io.github.kxyr0.echoquests.echoscript.binding;

import io.github.kxyr0.echoquests.api.identity.QuestEventId;
import io.github.kxyr0.echoquests.api.identity.QuestId;
import io.github.kxyr0.echoquests.api.identity.QuestStateId;
import io.github.kxyr0.echoquests.api.model.QuestDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateDefinition;
import io.github.kxyr0.echoquests.api.model.QuestStateKind;
import io.github.kxyr0.echoquests.api.model.QuestTimedTransition;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassCallSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class QuestResourceBinder {
    private static final Set<String> RESOURCE_CALLS = Set.of("Id", "Title", "Initial");
    private static final Set<String> STATE_CALLS = Set.of("Objective", "On", "At", "Complete", "Fail");
    private final QuestCallReader calls = new QuestCallReader();
    private final QuestConditionBinder conditions = new QuestConditionBinder(calls);

    public QuestDefinition bind(ClassSyntax resource, QuestSourceContext context) {
        if (!resource.baseType().name().text().equals("QuestResource")) throw failure("Expected QuestResource.");
        validateMembers(resource);
        QuestId id = id(resource, context);
        String title = required(resource, "Title");
        QuestStateId initial = new QuestStateId(required(resource, "Initial"));
        Map<QuestStateId, QuestStateDefinition> states = new LinkedHashMap<>();
        for (ClassSectionSyntax section : calls.sections(resource, "State")) {
            QuestStateDefinition state = state(section);
            if (states.putIfAbsent(state.id(), state) != null) throw failure("Duplicate State: " + state.id());
        }
        return new QuestDefinition(id, title, initial, states);
    }

    private QuestId id(ClassSyntax resource, QuestSourceContext context) {
        var values = calls.calls(resource, "Id");
        if (values.size() > 1) throw failure("Id declared more than once.");
        return values.isEmpty() ? context.inferredId() : new QuestId(singleString(values.get(0), "Id"));
    }

    private String required(ClassSyntax resource, String name) {
        var values = calls.calls(resource, name);
        if (values.size() != 1) throw failure(name + " must be declared exactly once.");
        return singleString(values.get(0), name);
    }

    private QuestStateDefinition state(ClassSectionSyntax section) {
        QuestStateId id = new QuestStateId(singleString(section.invocation(), "State"));
        String objective = "";
        QuestStateKind kind = QuestStateKind.ACTIVE;
        Map<QuestEventId, QuestStateId> transitions = new LinkedHashMap<>();
        List<QuestTimedTransition> timed = new ArrayList<>();
        boolean terminal = false;
        for (var statement : section.body().statements()) {
            if (!(statement instanceof ExpressionStatementSyntax expression)) {
                throw failure("State supports only Objective, On, At, Complete, and Fail calls.");
            }
            var call = calls.statement(expression).orElseThrow(() -> failure("Invalid State statement."));
            String name = calls.name(call);
            if (!STATE_CALLS.contains(name)) throw failure("Unknown State call: " + name);
            switch (name) {
                case "Objective" -> {
                    if (!objective.isEmpty()) throw failure("Objective declared more than once in " + id);
                    objective = singleString(call, "Objective");
                }
                case "On" -> transition(call, transitions);
                case "At" -> timed.add(timed(call));
                case "Complete" -> { ensureNoArguments(call, name); kind = QuestStateKind.COMPLETED; terminal = terminal(terminal, id); }
                case "Fail" -> { ensureNoArguments(call, name); kind = QuestStateKind.FAILED; terminal = terminal(terminal, id); }
                default -> throw failure("Unknown State call: " + name);
            }
        }
        return new QuestStateDefinition(id, objective, kind, transitions, timed);
    }

    private QuestTimedTransition timed(
            io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax call) {
        if (calls.positionalCount(call) != 1) throw failure("At requires one HH:mm argument.");
        var unknown = calls.namedArguments(call).stream()
                .filter(name -> !Set.of("when", "then", "otherwise").contains(name)).findFirst();
        if (unknown.isPresent()) throw failure("Unknown At argument: " + unknown.orElseThrow());
        String time = calls.string(call, 0).orElseThrow(() -> failure("At time must be a string."));
        var condition = calls.named(call, "when").orElseThrow(() -> failure("At requires when."));
        QuestStateId satisfied = new QuestStateId(calls.namedString(call, "then")
                .orElseThrow(() -> failure("At requires string then.")));
        QuestStateId otherwise = new QuestStateId(calls.namedString(call, "otherwise")
                .orElseThrow(() -> failure("At requires string otherwise.")));
        return new QuestTimedTransition(time, conditions.bind(condition), satisfied, otherwise);
    }

    private void transition(io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax call,
            Map<QuestEventId, QuestStateId> transitions) {
        if (calls.positionalCount(call) != 2) throw failure("On requires event and target state.");
        QuestEventId event = new QuestEventId(calls.string(call, 0)
                .orElseThrow(() -> failure("On event must be a string.")));
        QuestStateId target = new QuestStateId(calls.string(call, 1)
                .orElseThrow(() -> failure("On target must be a string.")));
        if (transitions.putIfAbsent(event, target) != null) throw failure("Duplicate event transition: " + event);
    }

    private void validateMembers(ClassSyntax resource) {
        for (var member : resource.members()) {
            if (member instanceof ClassCallSyntax call && RESOURCE_CALLS.contains(calls.name(call.invocation()))) continue;
            if (member instanceof ClassSectionSyntax section && calls.name(section.invocation()).equals("State")) continue;
            throw failure("QuestResource contains an unsupported member.");
        }
    }

    private String singleString(io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax call,
            String name) {
        if (calls.positionalCount(call) != 1) throw failure(name + " requires one string argument.");
        return calls.string(call, 0).orElseThrow(() -> failure(name + " argument must be a string."));
    }

    private void ensureNoArguments(io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax call,
            String name) {
        if (!call.arguments().isEmpty()) throw failure(name + " does not accept arguments.");
    }

    private boolean terminal(boolean existing, QuestStateId id) {
        if (existing) throw failure("State has more than one terminal marker: " + id);
        return true;
    }

    private IllegalArgumentException failure(String message) { return new IllegalArgumentException(message); }
}
