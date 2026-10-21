package io.github.kxyr0.echoanimation.forge.client.portrait;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the small EchoScript-shaped PortraitOverlay(...) config call.
 */
final class PortraitOverlayConfigParser {
    private static final Pattern OVERLAY_CALL = Pattern.compile("\\bPortraitOverlay\\s*\\(", Pattern.DOTALL);
    private static final Pattern LAYER_CALL = Pattern.compile("\\bLayer\\s*\\(", Pattern.DOTALL);
    private static final Pattern BLINK_BLOCK = Pattern.compile(
            "\\bBlink\\s*\"([A-Za-z0-9_.:/-]+)\"\\s*\\{", Pattern.DOTALL);
    private static final Pattern FIRST_STRING = Pattern.compile("\"([A-Za-z0-9_.:/-]+)\"");
    private static final Pattern NUMBER = Pattern.compile("-?[0-9]+(?:\\.[0-9]+)?");
    private static final Pattern ARGUMENT = Pattern.compile(
            "([A-Za-z][A-Za-z0-9_]*)\\s*:\\s*(true|false|\"[^\"]*\"|-?[0-9]+(?:\\.[0-9]+)?)");

    Optional<Entry> parse(String source) {
        Optional<String> body = callBody(source, OVERLAY_CALL);
        if (body.isEmpty()) {
            return Optional.empty();
        }
        Matcher id = FIRST_STRING.matcher(body.orElseThrow());
        if (!id.find()) {
            return Optional.empty();
        }
        String globals = globalArguments(body.orElseThrow());
        Map<String, String> arguments = arguments(globals);
        PortraitOverlayConfig defaults = PortraitOverlayConfig.defaultConfig();
        return Optional.of(new Entry(id.group(1), new PortraitOverlayConfig(
                number(arguments, "centerX", defaults.centerX()),
                number(arguments, "centerY", defaults.centerY()),
                number(arguments, "offsetX", defaults.offsetX()),
                number(arguments, "offsetY", defaults.offsetY()),
                number(arguments, "facePixels", defaults.facePixels()),
                layers(body.orElseThrow()),
                blinks(body.orElseThrow()))));
    }

    private Map<String, String> arguments(String source) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = ARGUMENT.matcher(source);
        while (matcher.find()) {
            result.put(matcher.group(1), unquote(matcher.group(2)));
        }
        return result;
    }

    private Map<String, PortraitLayerConfig> layers(String source) {
        Map<String, PortraitLayerConfig> result = new HashMap<>();
        Matcher matcher = LAYER_CALL.matcher(source);
        while (matcher.find()) {
            callBody(source, matcher.start()).ifPresent(body -> parseLayer(body, result));
        }
        return result;
    }

    private void parseLayer(String body, Map<String, PortraitLayerConfig> result) {
        Matcher id = FIRST_STRING.matcher(body);
        if (!id.find()) {
            return;
        }
        Map<String, String> arguments = arguments(body);
        result.put(id.group(1), new PortraitLayerConfig(
                optionalNumber(arguments, "placeX"),
                optionalNumber(arguments, "placeY"),
                optionalNumber(arguments, "offsetX"),
                optionalNumber(arguments, "offsetY"),
                optionalNumber(arguments, "scaleX"),
                optionalNumber(arguments, "scaleY"),
                optionalNumber(arguments, "w"),
                optionalNumber(arguments, "h"),
                optionalBoolean(arguments, "visible")));
    }

    private Map<String, PortraitBlinkConfig> blinks(String source) {
        Map<String, PortraitBlinkConfig> result = new HashMap<>();
        Matcher matcher = BLINK_BLOCK.matcher(source);
        while (matcher.find()) {
            String id = matcher.group(1);
            braceBody(source, matcher.start()).ifPresent(body -> parseBlink(id, body, result));
        }
        return result;
    }

    private void parseBlink(String id, String body, Map<String, PortraitBlinkConfig> result) {
        Optional<double[]> travel = numberPair(body, "Travel");
        Optional<double[]> gaze = numberPair(body, "Gaze");
        result.put(id, new PortraitBlinkConfig(
                travel.map(value -> value[0]).orElse(null),
                travel.map(value -> value[1]).orElse(null),
                gaze.map(value -> value[0]).orElse(null),
                gaze.map(value -> value[1]).orElse(null),
                booleanCall(body, "Idle").orElse(null),
                integerCall(body, "PeriodTicks").orElse(null),
                integerCall(body, "CloseTicks").orElse(null),
                integerCall(body, "HoldTicks").orElse(null),
                integerCall(body, "OpenTicks").orElse(null)));
    }

    private Optional<String> callBody(String source, Pattern pattern) {
        Matcher matcher = pattern.matcher(source);
        return matcher.find() ? callBody(source, matcher.start()) : Optional.empty();
    }

    private Optional<String> callBody(String source, int start) {
        int open = source.indexOf('(', start);
        int close = closingParen(source, open);
        return open < 0 || close < 0 ? Optional.empty() : Optional.of(source.substring(open + 1, close));
    }

    private Optional<String> braceBody(String source, int start) {
        int open = source.indexOf('{', start);
        int close = closingBrace(source, open);
        return open < 0 || close < 0 ? Optional.empty() : Optional.of(source.substring(open + 1, close));
    }

    private int closingParen(String source, int open) {
        int depth = 0;
        for (int index = open; index < source.length(); index++) {
            char value = source.charAt(index);
            if (value == '(') {
                depth++;
            } else if (value == ')' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private int closingBrace(String source, int open) {
        int depth = 0;
        for (int index = open; index < source.length(); index++) {
            char value = source.charAt(index);
            if (value == '{') {
                depth++;
            } else if (value == '}' && --depth == 0) {
                return index;
            }
        }
        return -1;
    }

    private String globalArguments(String source) {
        int layersIndex = source.indexOf("layers");
        return layersIndex < 0 ? source : source.substring(0, layersIndex);
    }

    private double number(Map<String, String> arguments, String name, double fallback) {
        String value = arguments.get(name);
        return value == null ? fallback : Double.parseDouble(value);
    }

    private Double optionalNumber(Map<String, String> arguments, String name) {
        String value = arguments.get(name);
        return value == null ? null : Double.valueOf(Double.parseDouble(value));
    }

    private Optional<double[]> numberPair(String source, String name) {
        return callBody(source, Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(", Pattern.DOTALL))
                .map(this::numbers).filter(values -> values.length >= 2)
                .map(values -> new double[] {values[0], values[1]});
    }

    private Optional<Integer> integerCall(String source, String name) {
        return callBody(source, Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(", Pattern.DOTALL))
                .map(this::numbers).filter(values -> values.length >= 1)
                .map(values -> Integer.valueOf((int) Math.round(values[0])));
    }

    private Optional<Boolean> booleanCall(String source, String name) {
        return callBody(source, Pattern.compile("\\b" + Pattern.quote(name) + "\\s*\\(", Pattern.DOTALL))
                .map(String::trim)
                .map(Boolean::parseBoolean);
    }

    private double[] numbers(String source) {
        Matcher matcher = NUMBER.matcher(source);
        java.util.ArrayList<Double> values = new java.util.ArrayList<>();
        while (matcher.find()) { values.add(Double.valueOf(Double.parseDouble(matcher.group()))); }
        double[] result = new double[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index).doubleValue();
        }
        return result;
    }

    private Boolean optionalBoolean(Map<String, String> arguments, String name) {
        String value = arguments.get(name);
        return value == null ? null : Boolean.valueOf(Boolean.parseBoolean(value));
    }

    private String unquote(String value) {
        return value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    record Entry(String visualId, PortraitOverlayConfig config) {
    }
}
