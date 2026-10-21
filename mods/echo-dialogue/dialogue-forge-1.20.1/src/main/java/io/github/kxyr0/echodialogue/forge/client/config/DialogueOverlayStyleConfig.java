package io.github.kxyr0.echodialogue.forge.client.config;

/**
 * Runtime style values for the compact dialogue overlay.
 */
public record DialogueOverlayStyleConfig(
        float textScale,
        boolean textShadow,
        int textAppearDelayMs,
        int typewriterMsPerCharacter,
        int typewriterMinDurationMs,
        int typewriterMaxDurationMs,
        float anchorX,
        float anchorY,
        int anchorOffsetX,
        int anchorOffsetY,
        int crosshairPaddingY,
        int screenMarginX,
        int screenMarginY,
        int minTextWidth,
        int maxTextWidth,
        int paddingX,
        int paddingY,
        int lineGap,
        int choiceSectionGap,
        int choicePaddingX,
        int choicePaddingY,
        int choiceGap,
        int visualSize,
        int visualGap,
        int visualTextOffsetY,
        int visualOffsetX,
        int visualOffsetY,
        int backgroundColor,
        int choiceBackgroundColor,
        int choiceBorderColor,
        int speakerColor,
        int textColor,
        int choiceNumberColor,
        int choiceTextColor
) {
    private static final DialogueOverlayStyleConfig DEFAULTS = new DialogueOverlayStyleConfig(
            1.0F, true, 80, 34, 450, 5000,
            0.5F, 0.5F, 0, 20, 10, 8, 8,
            30, 256, 8, 4, 2, 7, 6, 4, 4, 28, 8, 1, 0, 0,
            0xB0101010, 0x80181818, 0x806A6A6A, 0xFF6A00, 0xFFFFFF, 0xFF6A00, 0xFFE6E6E6);

    public DialogueOverlayStyleConfig {
        requirePositive(textScale, "textScale");
        requireNonNegative(textAppearDelayMs, "textAppearDelayMs");
        requirePositive(typewriterMsPerCharacter, "typewriterMsPerCharacter");
        requireNonNegative(typewriterMinDurationMs, "typewriterMinDurationMs");
        requireNonNegative(typewriterMaxDurationMs, "typewriterMaxDurationMs");
        requireNonNegative(crosshairPaddingY, "crosshairPaddingY");
        requireNonNegative(screenMarginX, "screenMarginX");
        requireNonNegative(screenMarginY, "screenMarginY");
        requirePositive(minTextWidth, "minTextWidth");
        requirePositive(maxTextWidth, "maxTextWidth");
        requireNonNegative(paddingX, "paddingX");
        requireNonNegative(paddingY, "paddingY");
        requireNonNegative(lineGap, "lineGap");
        requireNonNegative(choiceSectionGap, "choiceSectionGap");
        requireNonNegative(choicePaddingX, "choicePaddingX");
        requireNonNegative(choicePaddingY, "choicePaddingY");
        requireNonNegative(choiceGap, "choiceGap");
        requirePositive(visualSize, "visualSize");
        requireNonNegative(visualGap, "visualGap");
        if (typewriterMinDurationMs > typewriterMaxDurationMs) {
            throw new IllegalArgumentException("typewriterMinDurationMs не должен быть больше max.");
        }
        if (minTextWidth > maxTextWidth) {
            throw new IllegalArgumentException("minTextWidth не должен быть больше maxTextWidth.");
        }
    }

    public static DialogueOverlayStyleConfig defaults() {
        return DEFAULTS;
    }

    private static void requirePositive(float value, String name) {
        if (!Float.isFinite(value) || value <= 0.0F) {
            throw new IllegalArgumentException(name + " должен быть положительным.");
        }
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " должен быть положительным.");
        }
    }

    private static void requireNonNegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " не должен быть отрицательным.");
        }
    }
}
