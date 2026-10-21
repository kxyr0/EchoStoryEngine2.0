package io.github.kxyr0.echodialogue.echoscript.binding.command.npc;

import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class NpcStateArguments {
    public Map<String, String> form(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver) {
        Map<String, String> values = stateBase(call, actorResolver);
        ArgumentValues.copy(values, "arg1", "form");
        ArgumentValues.copy(values, "formId", "form");
        values.computeIfPresent("form", (key, value) -> fieldResolver.apply(value).orElse(value));
        clean(values, "formId");
        return Map.copyOf(values);
    }

    public Map<String, String> skin(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver) {
        Map<String, String> values = stateBase(call, actorResolver);
        ArgumentValues.copy(values, "arg1", "skin");
        ArgumentValues.copy(values, "skinId", "skin");
        values.computeIfPresent("skin", (key, value) -> fieldResolver.apply(value).orElse(value));
        clean(values, "skinId");
        return Map.copyOf(values);
    }

    public Map<String, String> sleep(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver,
            Function<String, Optional<String>> fieldResolver) {
        Map<String, String> values = stateBase(call, actorResolver);
        ArgumentValues.copy(values, "arg1", "bedX");
        ArgumentValues.copy(values, "arg2", "bedY");
        ArgumentValues.copy(values, "arg3", "bedZ");
        ArgumentValues.copy(values, "x", "bedX");
        ArgumentValues.copy(values, "y", "bedY");
        ArgumentValues.copy(values, "z", "bedZ");
        ArgumentValues.copy(values, "dimension", "worldId");
        ArgumentValues.copy(values, "world", "worldId");
        values.computeIfAbsent("worldId", ignored -> "minecraft:overworld");
        values.computeIfAbsent("expectedBlock", ignored -> "echo_npc:any_bed");
        values.computeIfPresent("worldId", (key, value) -> fieldResolver.apply(value).orElse(value));
        values.computeIfPresent("expectedBlock", (key, value) -> fieldResolver.apply(value).orElse(value));
        clean(values, "x", "y", "z", "dimension", "world");
        return Map.copyOf(values);
    }

    public Map<String, String> wake(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver) {
        Map<String, String> values = stateBase(call, actorResolver);
        clean(values);
        return Map.copyOf(values);
    }

    private Map<String, String> stateBase(
            InvocationExpressionSyntax call,
            Function<String, String> actorResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copy(values, "actor", "npc");
        ArgumentValues.copy(values, "actorId", "npc");
        ArgumentValues.copy(values, "npcId", "npc");
        ArgumentValues.copy(values, "target", "npc");
        values.computeIfPresent("npc", (key, value) -> actorResolver.apply(value));
        return values;
    }

    private void clean(Map<String, String> values, String... extraAliases) {
        ArgumentValues.removePositionals(values);
        ArgumentValues.remove(values, "actor", "actorId", "npcId", "target");
        ArgumentValues.remove(values, extraAliases);
    }
}
