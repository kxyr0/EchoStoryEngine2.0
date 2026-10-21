package io.github.kxyr0.echodialogue.echoscript.binding.command.core;

import java.util.Map;
import java.util.Optional;

public final class CommandChecks {
    private CommandChecks() {
    }

    public static boolean required(Map<String, String> arguments, String... names) {
        for (String name : names) {
            if (!arguments.containsKey(name) || arguments.get(name).isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static Optional<Integer> positiveInt(String value) {
        return intValue(value).filter(number -> number > 0);
    }

    public static Optional<Integer> nonNegativeInt(String value) {
        return intValue(value).filter(number -> number >= 0);
    }

    public static boolean positiveDouble(String value) {
        try {
            return Double.parseDouble(value) > 0.0D;
        } catch (NumberFormatException | NullPointerException exception) {
            return false;
        }
    }

    public static Optional<String> errorUnless(boolean condition, String message) {
        return condition ? Optional.empty() : Optional.of(message);
    }

    public static boolean nonNegativeValue(Map<String, String> arguments) {
        return required(arguments, "value")
                && nonNegativeInt(arguments.get("value")).isPresent();
    }

    public static boolean validBlinkTiming(Map<String, String> arguments) {
        Optional<Integer> period = positiveInt(arguments.get("periodTicks"));
        Optional<Integer> close = positiveInt(arguments.get("closeTicks"));
        Optional<Integer> hold = nonNegativeInt(arguments.get("holdTicks"));
        Optional<Integer> open = positiveInt(arguments.get("openTicks"));
        return period.isPresent()
                && close.isPresent()
                && hold.isPresent()
                && open.isPresent()
                && period.orElseThrow()
                        > close.orElseThrow() + hold.orElseThrow() + open.orElseThrow();
    }

    private static Optional<Integer> intValue(String value) {
        try {
            return Optional.of(Integer.parseInt(value));
        } catch (NumberFormatException | NullPointerException exception) {
            return Optional.empty();
        }
    }
}
