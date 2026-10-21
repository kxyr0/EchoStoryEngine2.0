package io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.sleep;

import io.github.kxyr0.echoscript.api.source.SourceSpan;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.IntrinsicCallReader;
import io.github.kxyr0.echostoryengine.story.echoscript.intrinsic.NodeSpec;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class SleepBlinkIntrinsics {
    private static final Set<String> CONTEXT = Set.of("player", "scenario", "owner", "command");

    List<SleepActionIntrinsics.LinearAction> linear(IntrinsicCallReader reader, SourceSpan span) {
        String intrinsic = reader.targetName();
        reader.requirePositionalCount(intrinsic, Set.of(0, 1));
        reader.rejectUnknownNamed(intrinsic, allowed());
        Optional<Integer> count = count(reader, intrinsic);
        if (count.isEmpty()) {
            return List.of();
        }
        String duration = value(reader, "duration", intrinsic, "8");
        String closed = value(reader, "closedWait", intrinsic, "0.12");
        String open = value(reader, "openWait", intrinsic, "0.18");
        String sleepy = value(reader, "sleepyBlink", intrinsic, "true");
        String hud = value(reader, "showHud", intrinsic, "false");
        List<SleepActionIntrinsics.LinearAction> actions = new ArrayList<>();
        for (int index = 0; index < count.orElseThrow(); index++) {
            actions.add(action("blink_close_eyes", span, "echo_player_sleep:close_eyes",
                    args("duration", duration, reader, intrinsic)));
            actions.add(action("blink_wait_until_closed", span, "echo_player_sleep:wait_until_closed",
                    context(reader, intrinsic)));
            actions.add(action("blink_wait_closed", span, "echo_player_sleep:wait",
                    args("seconds", closed, reader, intrinsic)));
            Map<String, String> openArgs = args("duration", duration, reader, intrinsic);
            openArgs.put("sleepyBlink", sleepy); openArgs.put("showHud", hud);
            actions.add(action("blink_open_eyes", span, "echo_player_sleep:open_eyes", openArgs));
            actions.add(action("blink_wait_until_open", span, "echo_player_sleep:wait_until_open",
                    context(reader, intrinsic)));
            actions.add(action("blink_wait_open", span, "echo_player_sleep:wait",
                    args("seconds", open, reader, intrinsic)));
        }
        return List.copyOf(actions);
    }

    private Set<String> allowed() {
        LinkedHashSet<String> values = new LinkedHashSet<>(Set.of(
                "count", "duration", "closedWait", "openWait", "sleepyBlink", "showHud"));
        values.addAll(CONTEXT);
        return Set.copyOf(values);
    }

    private Optional<Integer> count(IntrinsicCallReader reader, String intrinsic) {
        Optional<String> value = reader.positionalCount() > 0
                ? reader.positionalScalar(0, intrinsic, "count")
                : reader.requiredNamedScalar("count", intrinsic);
        try {
            int parsed = Integer.parseInt(value.orElse(""));
            if (parsed > 0) {
                return Optional.of(parsed);
            }
        } catch (NumberFormatException ignored) {
            // The structured invalid-count diagnostic is emitted below.
        }
        reader.report("ESE-SOURCE-024", Map.of(
                "intrinsic", intrinsic, "argument", "count", "expected", "positive-integer"));
        return Optional.empty();
    }

    private String value(IntrinsicCallReader reader, String name, String intrinsic, String fallback) {
        return reader.optionalNamedScalar(name, intrinsic).orElse(fallback);
    }

    private SleepActionIntrinsics.LinearAction action(
            String id, SourceSpan span, String type, Map<String, String> arguments) {
        NodeSpec spec = NodeSpec.action(span, new NodeSpec.ActionFields(
                Optional.empty(), Optional.of(type), Optional.empty(), arguments));
        return new SleepActionIntrinsics.LinearAction(id, spec);
    }

    private Map<String, String> args(String name, String value,
                                     IntrinsicCallReader reader, String intrinsic) {
        Map<String, String> arguments = context(reader, intrinsic);
        arguments.put(name, value);
        return arguments;
    }

    private Map<String, String> context(IntrinsicCallReader reader, String intrinsic) {
        Map<String, String> arguments = new LinkedHashMap<>();
        CONTEXT.forEach(name -> reader.optionalNamedScalar(name, intrinsic)
                .ifPresent(value -> arguments.put(name, value)));
        return arguments;
    }
}
