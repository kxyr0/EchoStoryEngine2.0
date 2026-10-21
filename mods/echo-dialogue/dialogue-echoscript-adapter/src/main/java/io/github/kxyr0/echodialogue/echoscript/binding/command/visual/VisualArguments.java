package io.github.kxyr0.echodialogue.echoscript.binding.command.visual;

import io.github.kxyr0.echodialogue.echoscript.binding.command.core.ArgumentValues;
import io.github.kxyr0.echoscript.api.syntax.expression.InvocationExpressionSyntax;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class VisualArguments {
    public Map<String, String> face(
            InvocationExpressionSyntax call,
            Function<String, String> subjectResolver,
            Function<String, Optional<String>> fieldResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "subject");
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copy(values, "arg1", "animation");
        ArgumentValues.copy(values, "arg1", "animationId");
        ArgumentValues.copy(values, "npc", "subject");
        ArgumentValues.copy(values, "npcId", "subject");
        ArgumentValues.copy(values, "actor", "subject");
        ArgumentValues.copy(values, "faceAnimation", "animation");
        ArgumentValues.copy(values, "animationId", "animation");
        ArgumentValues.copy(values, "faceAnimationId", "animation");
        values.computeIfPresent("subject", (key, value) -> subjectResolver.apply(value));
        values.computeIfPresent(
                "animation",
                (key, value) -> fieldResolver.apply(value).orElse(value));
        ArgumentValues.removePositionals(values);
        ArgumentValues.remove(
                values,
                "npc", "npcId", "actor",
                "animationId", "faceAnimation", "faceAnimationId");
        return Map.copyOf(values);
    }

    public Map<String, String> clearFace(
            InvocationExpressionSyntax call,
            Function<String, String> subjectResolver,
            boolean all) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        if (!all) {
            ArgumentValues.copy(values, "arg0", "subject");
            ArgumentValues.copy(values, "arg0", "npc");
            ArgumentValues.copy(values, "npc", "subject");
            ArgumentValues.copy(values, "npcId", "subject");
            ArgumentValues.copy(values, "actor", "subject");
            values.computeIfPresent("subject", (key, value) -> subjectResolver.apply(value));
            ArgumentValues.remove(values, "npc", "npcId", "actor");
        }
        ArgumentValues.removePositionals(values);
        return Map.copyOf(values);
    }

    public Map<String, String> blink(
            InvocationExpressionSyntax call,
            Function<String, String> subjectResolver) {
        Map<String, String> values = new LinkedHashMap<>(ArgumentValues.read(call));
        ArgumentValues.copy(values, "arg0", "subject");
        ArgumentValues.copy(values, "arg0", "npc");
        ArgumentValues.copy(values, "arg1", "periodTicks");
        ArgumentValues.copy(values, "arg2", "closeTicks");
        ArgumentValues.copy(values, "arg3", "holdTicks");
        ArgumentValues.copy(values, "arg4", "openTicks");
        ArgumentValues.copy(values, "npc", "subject");
        ArgumentValues.copy(values, "npcId", "subject");
        ArgumentValues.copy(values, "actor", "subject");
        ArgumentValues.copy(values, "period", "periodTicks");
        ArgumentValues.copy(values, "close", "closeTicks");
        ArgumentValues.copy(values, "hold", "holdTicks");
        ArgumentValues.copy(values, "open", "openTicks");
        values.computeIfPresent("subject", (key, value) -> subjectResolver.apply(value));
        ArgumentValues.removePositionals(values);
        ArgumentValues.remove(
                values,
                "npc", "npcId", "actor",
                "period", "close", "hold", "open");
        return Map.copyOf(values);
    }
}
