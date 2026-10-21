package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.time;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.definition.source.SourceNode;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.SourceNodeFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class TimeActionIntrinsics {
    public static final Set<String> TARGETS = Set.of(
            "Time.Set", "Time.Add", "Time.Pause", "Time.Resume", "Time.WaitUntil",
            "Time.Is", "Time.IsBefore", "Time.IsAfter", "Time.IsBetween");
    private static final Set<String> DURATIONS = Set.of("Minutes", "Hours");
    private final SourceNodeFactory nodes;

    public TimeActionIntrinsics(SourceNodeFactory nodes) { this.nodes = nodes; }

    public Optional<NodeSpec> linear(IntrinsicCallReader reader, SourceSpan span) {
        return action(reader, span, false);
    }

    public Optional<SourceNode> node(IntrinsicCallReader reader, SourceSpan span) {
        Optional<NodeSpec> spec = action(reader, span, true);
        return spec.flatMap(value -> nodes.node(reader, value));
    }

    public String id(IntrinsicCallReader reader) {
        return switch (reader.targetName()) {
            case "Time.Set" -> "time_set";
            case "Time.Add" -> "time_add";
            case "Time.Pause" -> "time_pause";
            case "Time.Resume" -> "time_resume";
            case "Time.WaitUntil" -> "time_wait_until";
            default -> "time_query";
        };
    }

    private Optional<NodeSpec> action(IntrinsicCallReader reader, SourceSpan span, boolean node) {
        String target = reader.targetName();
        int offset = node ? 1 : 0;
        Map<String, String> arguments = new LinkedHashMap<>();
        arguments.put("_sourceLine", Integer.toString(span.start().line()));
        accept(reader, target, node);
        switch (target) {
            case "Time.Set" -> set(reader, target, offset, arguments);
            case "Time.Add" -> add(reader, target, offset, arguments);
            case "Time.WaitUntil", "Time.Is", "Time.IsBefore", "Time.IsAfter" ->
                    put(arguments, "time", reader.positionalString(offset, target, "time"));
            case "Time.IsBetween" -> between(reader, target, offset, arguments);
            default -> { }
        }
        var fields = new NodeSpec.ActionFields(
                node ? reader.requiredNamedString("next", target) : Optional.empty(),
                Optional.of(type(target)), reader.optionalNamedString("output", target), arguments);
        return Optional.of(NodeSpec.action(span, fields));
    }

    private void set(IntrinsicCallReader reader, String target, int offset, Map<String, String> arguments) {
        int values = reader.positionalCount() - offset;
        if (values == 2) {
            reader.positionalInvocationScalar(offset, target, "day", Set.of("Day"))
                    .ifPresent(day -> arguments.put("day", day.value()));
        }
        int timeIndex = offset + (values == 2 ? 1 : 0);
        put(arguments, "time", reader.positionalString(timeIndex, target, "time"));
        arguments.put("crossingPolicy", reader.optionalNamedReference("crossed", target).orElse("SKIP_CROSSED"));
    }

    private void add(IntrinsicCallReader reader, String target, int offset, Map<String, String> arguments) {
        reader.positionalInvocationScalar(offset, target, "duration", DURATIONS).ifPresent(duration -> {
            arguments.put("durationUnit", duration.function());
            arguments.put("durationValue", duration.value());
        });
        arguments.put("crossingPolicy", reader.optionalNamedReference("crossed", target).orElse("FIRE_CROSSED"));
    }

    private void between(IntrinsicCallReader reader, String target, int offset, Map<String, String> arguments) {
        put(arguments, "start", reader.positionalString(offset, target, "start"));
        put(arguments, "end", reader.positionalString(offset + 1, target, "end"));
    }

    private void accept(IntrinsicCallReader reader, String target, boolean node) {
        int offset = node ? 1 : 0;
        Set<Integer> counts = switch (target) {
            case "Time.Set" -> Set.of(offset + 1, offset + 2);
            case "Time.Add", "Time.WaitUntil", "Time.Is", "Time.IsBefore", "Time.IsAfter" -> Set.of(offset + 1);
            case "Time.IsBetween" -> Set.of(offset + 2);
            default -> Set.of(offset);
        };
        reader.requirePositionalCount(target, counts);
        reader.rejectUnknownNamed(target, node ? Set.of("next", "output", "crossed")
                : Set.of("output", "crossed"));
    }

    private String type(String target) {
        return "echo_time:" + switch (target) {
            case "Time.Set" -> "set";
            case "Time.Add" -> "add";
            case "Time.Pause" -> "pause";
            case "Time.Resume" -> "resume";
            case "Time.WaitUntil" -> "wait_until";
            case "Time.Is" -> "is";
            case "Time.IsBefore" -> "is_before";
            case "Time.IsAfter" -> "is_after";
            case "Time.IsBetween" -> "is_between";
            default -> throw new IllegalArgumentException("Unknown Time intrinsic " + target);
        };
    }

    private void put(Map<String, String> values, String name, Optional<String> value) {
        value.ifPresent(present -> values.put(name, present));
    }
}
