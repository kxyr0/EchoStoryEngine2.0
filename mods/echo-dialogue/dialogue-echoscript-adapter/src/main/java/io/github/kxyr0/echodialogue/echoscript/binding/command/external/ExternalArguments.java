package io.github.kxyr0.echodialogue.echoscript.binding.command.external;

import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class ExternalArguments {
    public Map<String, String> delay(InvocationExpressionSyntax call) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "ticks");
        String ticks = values.get("ticks");
        return ticks == null ? Map.of() : Map.of("ticks", ticks);
    }

    public Map<String, String> cutscene(
            InvocationExpressionSyntax call,
            Function<String, Optional<String>> fieldResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "cutscene");
        ArgumentValues.copy(values, "id", "cutscene");
        ArgumentValues.copy(values, "cutsceneId", "cutscene");
        values.computeIfPresent(
                "cutscene",
                (key, value) -> fieldResolver.apply(value).orElse(value));
        ArgumentValues.removePositionals(values);
        return Map.copyOf(values);
    }

    public Map<String, String> player(
            InvocationExpressionSyntax call,
            String waitType) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        if ("echo_player_actor:rotate".equals(waitType)) {
            ArgumentValues.copy(values, "arg0", "yaw");
            ArgumentValues.copy(values, "arg1", "pitch");
        } else if ("echo_player_actor:set_hotbar_item".equals(waitType)) {
            ArgumentValues.copy(values, "arg0", "slot");
            ArgumentValues.copy(values, "arg1", "itemId");
        } else if ("echo_player_actor:select_hotbar_slot".equals(waitType)) {
            ArgumentValues.copy(values, "arg0", "slot");
        } else if (isFatigue(waitType)) {
            ArgumentValues.copy(values, "arg0", "value");
        } else {
            ArgumentValues.copy(values, "arg0", "x");
            ArgumentValues.copy(values, "arg1", "y");
            ArgumentValues.copy(values, "arg2", "z");
            if ("echo_player_actor:spawn_at".equals(waitType)) {
                ArgumentValues.copy(values, "arg3", "yaw");
                ArgumentValues.copy(values, "arg4", "pitch");
            }
        }
        if ("echo_player_actor:move".equals(waitType)) {
            values.computeIfAbsent("dimension", ignored -> "minecraft:overworld");
        }
        if ("echo_player_actor:interact".equals(waitType)) {
            values.computeIfAbsent("hand", ignored -> "MAIN_HAND");
            values.computeIfAbsent("maxDistance", ignored -> "4.5");
        }
        ArgumentValues.removePositionals(values);
        return Map.copyOf(values);
    }

    public Map<String, String> screen(InvocationExpressionSyntax call) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "canvasGroup");
        ArgumentValues.copy(values, "arg0", "alpha");
        ArgumentValues.copy(values, "arg1", "time");
        ArgumentValues.copy(values, "arg1", "durationMillis");
        ArgumentValues.copy(values, "alpha", "canvasGroup");
        ArgumentValues.copy(values, "durationMillis", "time");
        ArgumentValues.copy(values, "duration", "time");
        ArgumentValues.removePositionals(values);
        return Map.copyOf(values);
    }

    public Map<String, String> effect(InvocationExpressionSyntax call) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "effectId");
        ArgumentValues.copy(values, "id", "effectId");
        ArgumentValues.copy(values, "effect", "effectId");
        values.computeIfAbsent("intensity", ignored -> "1.0");
        ArgumentValues.removePositionals(values);
        ArgumentValues.remove(values, "id", "effect");
        return Map.copyOf(values);
    }

    private boolean isFatigue(String waitType) {
        return "echo_player_vitals:set_fatigue".equals(waitType)
                || "echo_player_vitals:add_fatigue".equals(waitType)
                || "echo_player_vitals:reduce_fatigue".equals(waitType);
    }
}
