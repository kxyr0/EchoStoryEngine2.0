package io.github.kxyr0.echodialogue.forge.client.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the EchoScript-shaped DialogueOverlayStyle(...) config call.
 */
final class DialogueOverlayStyleConfigParser {
    private static final Pattern CALL = Pattern.compile("DialogueOverlayStyle\\s*\\((.*?)\\)", Pattern.DOTALL);
    private static final Pattern ARGUMENT = Pattern.compile(
            "([A-Za-z][A-Za-z0-9_]*)\\s*:\\s*(true|false|\"[^\"]*\"|0x[0-9A-Fa-f]+|-?[0-9]+(?:\\.[0-9]+)?)",
            Pattern.CASE_INSENSITIVE);

    Optional<DialogueOverlayStyleConfig> parse(String source, DialogueOverlayStyleConfig fallback) {
        Matcher call = CALL.matcher(source);
        if (!call.find()) {
            return Optional.empty();
        }
        Map<String, String> args = arguments(call.group(1));
        return Optional.of(config(args, fallback));
    }

    private DialogueOverlayStyleConfig config(Map<String, String> args, DialogueOverlayStyleConfig fallback) {
        return new DialogueOverlayStyleConfig(
                floatValue(args, "textScale", fallback.textScale()),
                bool(args, "textShadow", fallback.textShadow()),
                intValue(args, "textAppearDelayMs", fallback.textAppearDelayMs()),
                intValue(args, "typewriterMsPerCharacter", fallback.typewriterMsPerCharacter()),
                intValue(args, "typewriterMinDurationMs", fallback.typewriterMinDurationMs()),
                intValue(args, "typewriterMaxDurationMs", fallback.typewriterMaxDurationMs()),
                floatValue(args, "anchorX", fallback.anchorX()),
                floatValue(args, "anchorY", fallback.anchorY()),
                intValue(args, "anchorOffsetX", fallback.anchorOffsetX()),
                intValue(args, "anchorOffsetY", fallback.anchorOffsetY()),
                intValue(args, "crosshairPaddingY", fallback.crosshairPaddingY()),
                intValue(args, "screenMarginX", fallback.screenMarginX()),
                intValue(args, "screenMarginY", fallback.screenMarginY()),
                intValue(args, "minTextWidth", fallback.minTextWidth()),
                intValue(args, "maxTextWidth", fallback.maxTextWidth()),
                intValue(args, "paddingX", fallback.paddingX()),
                intValue(args, "paddingY", fallback.paddingY()),
                intValue(args, "lineGap", fallback.lineGap()),
                intValue(args, "choiceSectionGap", fallback.choiceSectionGap()),
                intValue(args, "choicePaddingX", fallback.choicePaddingX()),
                intValue(args, "choicePaddingY", fallback.choicePaddingY()),
                intValue(args, "choiceGap", fallback.choiceGap()),
                intValue(args, "visualSize", fallback.visualSize()),
                intValue(args, "visualGap", fallback.visualGap()),
                intValue(args, "visualTextOffsetY", fallback.visualTextOffsetY()),
                intValue(args, "visualOffsetX", fallback.visualOffsetX()),
                intValue(args, "visualOffsetY", fallback.visualOffsetY()),
                color(args, "backgroundColor", fallback.backgroundColor()),
                color(args, "choiceBackgroundColor", fallback.choiceBackgroundColor()),
                color(args, "choiceBorderColor", fallback.choiceBorderColor()),
                color(args, "speakerColor", fallback.speakerColor()),
                color(args, "textColor", fallback.textColor()),
                color(args, "choiceNumberColor", fallback.choiceNumberColor()),
                color(args, "choiceTextColor", fallback.choiceTextColor()));
    }

    private Map<String, String> arguments(String source) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = ARGUMENT.matcher(source);
        while (matcher.find()) {
            result.put(matcher.group(1), unquote(matcher.group(2)));
        }
        return result;
    }

    private float floatValue(Map<String, String> args, String name, float fallback) {
        String value = args.get(name);
        return value == null ? fallback : Float.parseFloat(value);
    }

    private int intValue(Map<String, String> args, String name, int fallback) {
        String value = args.get(name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private boolean bool(Map<String, String> args, String name, boolean fallback) {
        String value = args.get(name);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private int color(Map<String, String> args, String name, int fallback) {
        String value = args.get(name);
        return value == null ? fallback : Long.decode(value).intValue();
    }

    private String unquote(String value) {
        return value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }
}
