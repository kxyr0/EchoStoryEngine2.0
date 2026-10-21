package io.github.kxyr0.echodialogue.echoscript.binding.command.npc;

import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

public final class NpcMovementArguments {
    public Map<String, String> movement(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "npc");
        if (ArgumentValues.copyInlineVector(call, values)) {
            ArgumentValues.copy(values, "arg2", "speed");
        } else {
            ArgumentValues.copy(values, "arg1", "x");
            ArgumentValues.copy(values, "arg2", "y");
            ArgumentValues.copy(values, "arg3", "z");
            ArgumentValues.copy(values, "arg4", "speed");
        }
        values.computeIfAbsent("dimension", ignored -> "minecraft:overworld");
        values.computeIfPresent("npc", (key, value) -> actorResolver.apply(value));
        ArgumentValues.removePositionals(values);
        return Map.copyOf(values);
    }

    public Map<String, String> arrival(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copy(values, "actor", "npc");
        ArgumentValues.copy(values, "actorId", "npc");
        ArgumentValues.copy(values, "npcId", "npc");
        ArgumentValues.copy(values, "subject", "npc");
        ArgumentValues.copy(values, "target", "npc");
        ArgumentValues.copy(values, "moveNpc", "npc");
        ArgumentValues.copy(values, "waitNpc", "npc");
        values.computeIfPresent("npc", (key, value) -> actorResolver.apply(value));
        return values.containsKey("npc")
                ? Map.of("npc", values.get("npc"))
                : Map.of();
    }
}
