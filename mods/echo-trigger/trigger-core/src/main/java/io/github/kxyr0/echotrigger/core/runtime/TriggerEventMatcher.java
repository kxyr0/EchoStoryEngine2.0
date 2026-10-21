package io.github.kxyr0.echotrigger.core.runtime;

import io.github.kxyr0.echotrigger.api.event.TriggerEvent;
import io.github.kxyr0.echotrigger.api.event.TriggerEventKind;
import io.github.kxyr0.echotrigger.api.model.TriggerCondition;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

final class TriggerEventMatcher {
    boolean matches(TriggerCondition condition, TriggerEvent event) {
        return switch (condition.expressionName()) {
            case "Any" -> condition.children().stream().anyMatch(child -> matches(child, event));
            case "All" -> condition.children().stream().allMatch(child -> matches(child, event));
            case "Not" -> condition.children().stream().noneMatch(child -> matches(child, event));
            default -> direct(condition, event);
        };
    }

    private boolean direct(TriggerCondition condition, TriggerEvent event) {
        if (condition.kind() != event.kind() || !areaMatches(condition, event) || !nameMatches(condition, event)) {
            return false;
        }
        if (condition.expressionName().equals("TimeBetween")
                && !TriggerTimeMatcher.between(condition.arguments(), event.payload())) {
            return false;
        }
        for (Map.Entry<String, String> argument : condition.arguments().entrySet()) {
            if (condition.expressionName().equals("TimeBetween")) {
                continue;
            }
            if (!argumentMatches(condition.expressionName(), argument.getKey(), argument.getValue(), event.payload())) {
                return false;
            }
        }
        return true;
    }

    private static boolean areaMatches(TriggerCondition condition, TriggerEvent event) {
        if (condition.area().isEmpty()) {
            return true;
        }
        if (condition.area().equals(event.area())) {
            return true;
        }
        return payloadPositionInsideArea(condition, event);
    }

    private static boolean payloadPositionInsideArea(TriggerCondition condition, TriggerEvent event) {
        String dimension = event.payload().get("dimension");
        String position = event.payload().get("position");
        if (dimension == null || position == null) {
            return false;
        }
        String[] parts = position.split(",");
        if (parts.length != 3) {
            return false;
        }
        try {
            return condition.area().orElseThrow().contains(dimension,
                    Double.parseDouble(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]));
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static boolean nameMatches(TriggerCondition condition, TriggerEvent event) {
        if (condition.kind() != TriggerEventKind.VANILLA_GAMEPLAY
                && condition.kind() != TriggerEventKind.PREDICATE_TRUE) {
            return true;
        }
        return condition.expressionName().equals(event.expressionName());
    }

    private boolean argumentMatches(
            String expressionName,
            String key,
            String expected,
            Map<String, String> payload) {
        if (key.equals("target")) {
            return targetMatches(expected, payload);
        }
        if (taggedTransitionTarget(key, expected)) {
            return tags(payload, key + "Tags").anyMatch(tag -> tagMatches(tag, expected));
        }
        Optional<String> actual = Optional.ofNullable(payload.get(key));
        return actual.filter(value -> scalarArgumentMatches(expressionName, key, expected, value, payload)).isPresent();
    }

    private static boolean targetMatches(String expected, Map<String, String> payload) {
        String actual = payload.getOrDefault("target", payload.getOrDefault("item",
                payload.getOrDefault("block", payload.getOrDefault("effect",
                        payload.getOrDefault("advancement", payload.getOrDefault("dimension", ""))))));
        if (!expected.startsWith("#")) {
            return expected.equals(actual);
        }
        return tags(payload, "tags").anyMatch(tag -> tagMatches(tag, expected));
    }

    private static java.util.stream.Stream<String> tags(Map<String, String> payload, String fallbackKey) {
        String raw = payload.getOrDefault("tags", payload.getOrDefault(fallbackKey,
                payload.getOrDefault("itemTags", payload.getOrDefault("blockTags", ""))));
        return Arrays.stream(raw.split(",")).map(String::trim).filter(value -> !value.isEmpty());
    }

    private static boolean numericKey(String key) {
        return key.equals("count") || key.equals("level") || key.equals("value") || key.equals("day")
                || key.equals("ticks") || key.equals("amplifier") || key.equals("amplifierAtLeast")
                || key.equals("amountAtLeast");
    }

    private static boolean numericExpression(String expressionName) {
        return expressionName.endsWith("AtLeast") || expressionName.endsWith("AtMost")
                || expressionName.endsWith("Exactly") || expressionName.endsWith("Reached");
    }

    private static boolean taggedTransitionTarget(String key, String expected) {
        return (key.equals("from") || key.equals("to")) && expected.startsWith("#");
    }

    private static boolean tagMatches(String tag, String expected) {
        return tag.equals(expected) || tag.equals(expected.substring(1));
    }

    private static boolean scalarArgumentMatches(
            String expressionName,
            String key,
            String expected,
            String actual,
            Map<String, String> payload) {
        if (key.equals("hand") && expected.equals("ANY_HAND")) {
            return true;
        }
        if (numericKey(key) || numericExpression(expressionName)) {
            return numericMatches(expressionName, key, expected, actual, payload);
        }
        return expected.equals(actual);
    }

    private static boolean numericMatches(
            String expressionName,
            String key,
            String expected,
            String actual,
            Map<String, String> payload) {
        double threshold;
        double value;
        try {
            threshold = Double.parseDouble(expected);
            value = Double.parseDouble(actual);
        } catch (NumberFormatException exception) {
            return false;
        }
        return numericMatches(expressionName, key, threshold, value, payload);
    }

    private static boolean numericMatches(
            String expressionName,
            String key,
            double threshold,
            double value,
            Map<String, String> payload) {
        if (expressionName.equals("TimeReached")) {
            return TriggerTimeMatcher.reached(threshold, value, payload);
        }
        if (expressionName.endsWith("Reached")) {
            return previousCrossedThreshold(threshold, key, payload) && value >= threshold;
        }
        if (expressionName.endsWith("AtMost")) {
            return value <= threshold;
        }
        if (expressionName.endsWith("Exactly")) {
            return Double.compare(value, threshold) == 0;
        }
        return key.equals("amountAtLeast") || key.equals("amplifierAtLeast") || expressionName.endsWith("AtLeast")
                ? value >= threshold
                : Double.compare(value, threshold) == 0;
    }

    private static boolean previousCrossedThreshold(double threshold, String key, Map<String, String> payload) {
        Optional<String> previous = Optional.ofNullable(payload.get("previous" + capitalize(key)))
                .or(() -> Optional.ofNullable(payload.get("previous")));
        if (previous.isEmpty()) {
            return false;
        }
        try {
            return Double.parseDouble(previous.orElseThrow()) < threshold;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static String capitalize(String value) {
        return value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

}
