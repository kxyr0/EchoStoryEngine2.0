package io.github.kxyr0.echodialogue.forge.client.config;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderMode;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiSliceBorders;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillDirection;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillSettings;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTileSettings;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the EchoScript-shaped DialoguePanelBackgroundOverlay(...) config call.
 */
final class DialoguePanelBackgroundConfigParser {
    private static final Pattern ARGUMENT = Pattern.compile(
            "([A-Za-z][A-Za-z0-9_]*)\\s*:\\s*(true|false|\"[^\"]*\"|[A-Za-z_][A-Za-z0-9_]*|-?[0-9]+(?:\\.[0-9]+)?)",
            Pattern.CASE_INSENSITIVE);
    private final Pattern call;

    DialoguePanelBackgroundConfigParser() {
        this("DialoguePanelBackgroundOverlay");
    }

    DialoguePanelBackgroundConfigParser(String callName) {
        call = Pattern.compile(Pattern.quote(callName) + "\\s*\\((.*?)\\)", Pattern.DOTALL);
    }

    Optional<DialoguePanelBackgroundConfig> parse(String source, DialoguePanelBackgroundConfig fallback) {
        Matcher matcher = call.matcher(source);
        if (!matcher.find()) {
            return Optional.empty();
        }
        Map<String, String> args = arguments(matcher.group(1));
        return Optional.of(config(args, fallback));
    }

    private DialoguePanelBackgroundConfig config(Map<String, String> args, DialoguePanelBackgroundConfig fallback) {
        String modeText = stringValue(args, "mode", fallback.mode().name());
        if ("solid".equalsIgnoreCase(modeText)) {
            return DialoguePanelBackgroundConfig.defaults();
        }
        return new DialoguePanelBackgroundConfig(
                true,
                UiImageRenderMode.parse(modeText),
                stringValue(args, "texture", fallback.texture()),
                intValue(args, "textureWidth", fallback.sourceWidth()),
                intValue(args, "textureHeight", fallback.sourceHeight()),
                borders(args, fallback.borders()),
                tileSettings(args, fallback.tileSettings()),
                fillSettings(args, fallback.fillSettings()));
    }

    private UiSliceBorders borders(Map<String, String> args, UiSliceBorders fallback) {
        return new UiSliceBorders(
                intValue(args, "sliceLeft", fallback.left()),
                intValue(args, "sliceRight", fallback.right()),
                intValue(args, "sliceTop", fallback.top()),
                intValue(args, "sliceBottom", fallback.bottom()));
    }

    private UiTileSettings tileSettings(Map<String, String> args, UiTileSettings fallback) {
        return new UiTileSettings(
                floatValue(args, "tileScaleX", fallback.scaleX()),
                floatValue(args, "tileScaleY", fallback.scaleY()),
                intValue(args, "tileOriginX", fallback.originX()),
                intValue(args, "tileOriginY", fallback.originY()),
                bool(args, "tileFullOnly", fallback.fullOnly()));
    }

    private UiFillSettings fillSettings(Map<String, String> args, UiFillSettings fallback) {
        return new UiFillSettings(
                UiFillDirection.parse(stringValue(args, "fillDirection", fallback.direction().name())),
                floatValue(args, "fillAmount", fallback.amount()));
    }

    private Map<String, String> arguments(String source) {
        Map<String, String> result = new HashMap<>();
        Matcher matcher = ARGUMENT.matcher(source);
        while (matcher.find()) {
            result.put(matcher.group(1), unquote(matcher.group(2)));
        }
        return result;
    }

    private int intValue(Map<String, String> args, String name, int fallback) {
        String value = args.get(name);
        return value == null ? fallback : Integer.parseInt(value);
    }

    private float floatValue(Map<String, String> args, String name, float fallback) {
        String value = args.get(name);
        return value == null ? fallback : Float.parseFloat(value);
    }

    private boolean bool(Map<String, String> args, String name, boolean fallback) {
        String value = args.get(name);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }

    private String stringValue(Map<String, String> args, String name, String fallback) {
        String value = args.get(name);
        return value == null ? fallback : value;
    }

    private String unquote(String value) {
        return value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }
}
