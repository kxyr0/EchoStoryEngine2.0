package io.github.kxyr0.echoiteminspection.client.render;

/** Physical framebuffer layout. GUI scale never selects the responsive arrangement. */
public record InspectionLayout(double modelX, double modelY, double modelSize,
        double textX, double textY, double textWidth, double textHeight,
        double textScale, boolean sideColumn, boolean hasDescription) {
    public static InspectionLayout resolve(InspectionViewport viewport, boolean description) {
        double width = viewport.framebufferWidth();
        double height = viewport.framebufferHeight();
        double margin = Math.max(16, Math.min(width, height) * 0.055);
        double reference = Math.min(width / 1920.0, height / 1080.0);
        double textScale = Math.max(1.25, reference * 2);
        if (!description) {
            return new InspectionLayout(width / 2, height * 0.44, Math.min(width, height) * 0.4,
                    margin, margin, width - 2 * margin, height * 0.20, textScale, false, false);
        }
        boolean side = width >= 960 && width / height >= 1.5;
        if (side) {
            double textWidth = Math.min(width * 0.34, reference * 760);
            double textX = width - margin - textWidth;
            double modelWidth = textX - 2 * margin;
            return new InspectionLayout(margin + modelWidth / 2, height * 0.44,
                    Math.min(modelWidth, height * 0.70) * 0.60, textX, height * 0.17,
                    textWidth, height * 0.57, textScale, true, true);
        }
        return new InspectionLayout(width / 2, height * 0.29,
                Math.min(width * 0.72, height * 0.46) * 0.80,
                margin, height * 0.56, width - 2 * margin, height * 0.18, textScale, false, true);
    }

    public double renderScale(InspectionViewport viewport, double authoredScale, double zoom) {
        double maximum = Math.min(viewport.framebufferWidth(), viewport.framebufferHeight()) * 4.0;
        return Math.min(maximum, modelSize * authoredScale * zoom) / viewport.guiScale();
    }
}
