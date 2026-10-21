package io.github.kxyr0.echodialogue.echoscript.binding.command.npc;

import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NpcInteractionArguments {
    public Map<String, String> npcOnly(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        copyNpcAliases(values);
        values.computeIfPresent("npc", (key, value) -> actorResolver.apply(value));
        cleanNpcAliases(values);
        return Map.copyOf(values);
    }

    public Map<String, String> interaction(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver,
            Function<String, Optional<Map<String, String>>> blockPositionResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copyBlockPosition(values, "arg1", blockPositionResolver);
        ArgumentValues.copyBlockPosition(values, "position", blockPositionResolver);
        ArgumentValues.copyBlockPosition(values, "block", blockPositionResolver);
        ArgumentValues.copyBlockPosition(values, "targetBlock", blockPositionResolver);
        ArgumentValues.copyInlineBlockPosition(call, values);
        ArgumentValues.copy(values, "arg1", "x");
        ArgumentValues.copy(values, "arg2", "y");
        ArgumentValues.copy(values, "arg3", "z");
        copyNpcAliases(values);
        values.computeIfAbsent("dimension", ignored -> "minecraft:overworld");
        values.computeIfAbsent("face", ignored -> "AUTO");
        values.computeIfAbsent("hand", ignored -> "MAIN_HAND");
        values.computeIfAbsent("approach", ignored -> "AUTO");
        values.computeIfPresent("npc", (key, value) -> actorResolver.apply(value));
        cleanNpcAliases(values);
        ArgumentValues.remove(values, "position", "block", "targetBlock");
        return Map.copyOf(values);
    }

    private void copyNpcAliases(Map<String, String> values) {
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copy(values, "actor", "npc");
        ArgumentValues.copy(values, "actorId", "npc");
        ArgumentValues.copy(values, "npcId", "npc");
        ArgumentValues.copy(values, "target", "npc");
    }

    private void cleanNpcAliases(Map<String, String> values) {
        ArgumentValues.removePositionals(values);
        ArgumentValues.remove(values, "actor", "actorId", "npcId", "target");
    }
}
