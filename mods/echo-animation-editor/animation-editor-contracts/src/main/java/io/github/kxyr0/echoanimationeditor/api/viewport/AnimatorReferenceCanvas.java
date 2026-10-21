package io.github.kxyr0.echoanimationeditor.api.viewport;

public record AnimatorReferenceCanvas(int width, int height) {
    public static final AnimatorReferenceCanvas FULL_HD = new AnimatorReferenceCanvas(1920, 1080);

    public AnimatorReferenceCanvas {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("reference canvas dimensions must be positive");
        }
    }
}
