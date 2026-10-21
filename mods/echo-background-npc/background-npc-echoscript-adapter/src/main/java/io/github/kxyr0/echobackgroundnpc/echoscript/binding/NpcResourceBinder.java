package io.github.kxyr0.echobackgroundnpc.echoscript.binding;

import io.github.kxyr0.echobackgroundnpc.api.definition.BackgroundNpcDefinition;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcDefinitionId;
import io.github.kxyr0.echobackgroundnpc.api.identity.agent.BackgroundNpcId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearanceId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundAppearancePoolId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundLocationId;
import io.github.kxyr0.echobackgroundnpc.api.identity.resource.BackgroundNpcProfileId;
import io.github.kxyr0.echobackgroundnpc.api.schedule.BackgroundScheduleEntry;
import io.github.kxyr0.echobackgroundnpc.api.task.BackgroundTask;
import io.github.kxyr0.echoscript.api.syntax.ClassSyntax;
import io.github.kxyr0.echoscript.api.syntax.declaration.ClassSectionSyntax;
import io.github.kxyr0.echoscript.api.syntax.statement.ExpressionStatementSyntax;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class NpcResourceBinder {
    private final BackgroundCallReader calls = new BackgroundCallReader();
    private final BackgroundTaskReader tasks = new BackgroundTaskReader();

    public BackgroundNpcDefinition bind(ClassSyntax resource, BackgroundResourceContext context, long revision,
            Map<String, BackgroundNpcId> symbols) {
        if (!resource.baseType().name().text().equals("BackgroundNpcResource")) {
            throw new IllegalArgumentException("Expected BackgroundNpcResource.");
        }
        BackgroundNpcId id = new BackgroundNpcId(context.resourceId());
        BackgroundLocationId home = new BackgroundLocationId(required(resource, "Home"));
        Optional<BackgroundNpcProfileId> profile = optional(resource, "Profile").map(BackgroundNpcProfileId::new);
        Optional<BackgroundAppearanceId> appearance = optional(resource, "Appearance")
                .map(BackgroundAppearanceId::new);
        Optional<BackgroundAppearancePoolId> pool = optional(resource, "AppearancePool")
                .map(BackgroundAppearancePoolId::new);
        Optional<Double> speed = calls.calls(resource, "WalkSpeed").stream().findFirst()
                .flatMap(value -> calls.number(value, 0));
        List<BackgroundScheduleEntry> schedule = calls.sections(resource, "At").stream()
                .map(section -> schedule(id, section, symbols)).sorted().toList();
        return new BackgroundNpcDefinition(id, new BackgroundNpcDefinitionId(id.value()), home, profile,
                appearance, pool, speed, schedule, calls.source(resource.span()), revision);
    }

    private BackgroundScheduleEntry schedule(BackgroundNpcId id, ClassSectionSyntax section,
            Map<String, BackgroundNpcId> symbols) {
        String time = calls.string(section.invocation(), 0)
                .orElseThrow(() -> new IllegalArgumentException("At requires HH:mm."));
        int minute = parseTime(time);
        if (section.body().statements().size() != 1
                || !(section.body().statements().get(0) instanceof ExpressionStatementSyntax statement)) {
            throw new IllegalArgumentException("At must contain exactly one task call.");
        }
        var invocation = calls.statement(statement)
                .orElseThrow(() -> new IllegalArgumentException("At body must contain a task call."));
        BackgroundTask task = tasks.read(invocation, symbols)
                .orElseThrow(() -> new IllegalArgumentException("Unknown task or Background NPC symbol."));
        String selector = "echo_background_npc/" + id.value() + "/" + minute;
        return new BackgroundScheduleEntry(minute, task, selector, calls.source(section.span()));
    }

    private int parseTime(String value) {
        if (!value.matches("(?:[01]\\d|2[0-3]):[0-5]\\d")) {
            throw new IllegalArgumentException("Schedule time must be HH:mm.");
        }
        return Integer.parseInt(value.substring(0, 2)) * 60 + Integer.parseInt(value.substring(3));
    }
    private String required(ClassSyntax resource, String name) {
        return optional(resource, name).orElseThrow(() -> new IllegalArgumentException(name + " is required."));
    }
    private Optional<String> optional(ClassSyntax resource, String name) {
        var values = calls.calls(resource, name);
        if (values.size() > 1) throw new IllegalArgumentException(name + " declared more than once.");
        return values.stream().findFirst().flatMap(value -> calls.string(value, 0));
    }
}
