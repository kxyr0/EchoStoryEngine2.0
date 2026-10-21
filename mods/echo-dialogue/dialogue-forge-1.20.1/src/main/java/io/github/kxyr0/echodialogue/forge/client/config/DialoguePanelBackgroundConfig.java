package io.github.kxyr0.echodialogue.forge.client.config;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageRenderMode;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiSliceBorders;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillDirection;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillSettings;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTileSettings;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Runtime-configurable dialogue panel background, loaded from a separate overlay.
 */
public record DialoguePanelBackgroundConfig(
        boolean enabled,
        UiImageRenderMode mode,
        String texture,
        int sourceWidth,
        int sourceHeight,
        UiSliceBorders borders,
        UiTileSettings tileSettings,
        UiFillSettings fillSettings
) {
    private static final Pattern RESOURCE_LOCATION =
            Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final DialoguePanelBackgroundConfig DEFAULTS = new DialoguePanelBackgroundConfig(
            false, UiImageRenderMode.SIMPLE, "echo_dialogue:textures/gui/panel/test_dialogue_panel.png",
            29, 28, new UiSliceBorders(6, 5, 6, 8),
            UiTileSettings.defaults(), UiFillSettings.full());

    public DialoguePanelBackgroundConfig {
        Objects.requireNonNull(mode, "mode");
        texture = requireResourceLocation(texture);
        requirePositive(sourceWidth, "sourceWidth");
        requirePositive(sourceHeight, "sourceHeight");
        Objects.requireNonNull(borders, "borders").validateInside(sourceWidth, sourceHeight);
        tileSettings = Objects.requireNonNullElse(tileSettings, UiTileSettings.defaults());
        fillSettings = Objects.requireNonNullElse(fillSettings, UiFillSettings.full());
    }

    public static DialoguePanelBackgroundConfig defaults() {
        return DEFAULTS;
    }

    public DialoguePanelBackgroundConfig withMode(UiImageRenderMode nextMode) {
        return new DialoguePanelBackgroundConfig(true, nextMode, texture, sourceWidth, sourceHeight,
                borders, tileSettings, fillSettingsFor(nextMode));
    }

    private UiFillSettings fillSettingsFor(UiImageRenderMode nextMode) {
        if (nextMode == UiImageRenderMode.FILLED) {
            return new UiFillSettings(UiFillDirection.LEFT_TO_RIGHT, 0.55F);
        }
        return fillSettings;
    }

    private static String requireResourceLocation(String value) {
        String text = Objects.requireNonNull(value, "texture").trim();
        if (!RESOURCE_LOCATION.matcher(text).matches()) {
            throw new IllegalArgumentException("texture must be a full namespace:path ResourceLocation.");
        }
        return text;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive.");
        }
    }
}
