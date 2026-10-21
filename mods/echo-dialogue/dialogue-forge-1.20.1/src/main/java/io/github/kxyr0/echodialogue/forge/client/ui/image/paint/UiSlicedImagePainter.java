package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiSliceBorders;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import java.util.ArrayList;
import java.util.List;

/**
 * Preserves four corners and stretches edges/center.
 */
public final class UiSlicedImagePainter {
    private UiSlicedImagePainter() {
    }

    public static List<UiImageDrawCommand> commands(UiImageDefinition image, UiImageBounds bounds) {
        requireBordersFit(image.borders(), bounds);
        List<UiImageDrawCommand> commands = new ArrayList<>(9);
        appendSlices(commands, image, bounds);
        return List.copyOf(commands);
    }

    static void requireBordersFit(UiSliceBorders border, UiImageBounds bounds) {
        if (bounds.width() < border.minimumWidth()) {
            throw new IllegalArgumentException("UI_IMAGE_DESTINATION_TOO_SMALL: width is smaller than borders.");
        }
        if (bounds.height() < border.minimumHeight()) {
            throw new IllegalArgumentException("UI_IMAGE_DESTINATION_TOO_SMALL: height is smaller than borders.");
        }
    }

    private static void appendSlices(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds
    ) {
        UiSliceBorders b = image.borders();
        UiTextureRegion r = image.region();
        int centerSourceWidth = r.width() - b.minimumWidth();
        int centerSourceHeight = r.height() - b.minimumHeight();
        int centerTargetWidth = bounds.width() - b.minimumWidth();
        int centerTargetHeight = bounds.height() - b.minimumHeight();
        int rightX = bounds.right() - b.right();
        int bottomY = bounds.bottom() - b.bottom();
        int rightSourceX = r.right() - b.right();
        int bottomSourceY = r.bottom() - b.bottom();
        add(commands, image, new TargetRect(bounds.x(), bounds.y(), b.left(), b.top()),
                new SourceRect(r.x(), r.y(), b.left(), b.top()));
        add(commands, image, new TargetRect(bounds.x() + b.left(), bounds.y(), centerTargetWidth, b.top()),
                new SourceRect(r.x() + b.left(), r.y(), centerSourceWidth, b.top()));
        add(commands, image, new TargetRect(rightX, bounds.y(), b.right(), b.top()),
                new SourceRect(rightSourceX, r.y(), b.right(), b.top()));
        add(commands, image, new TargetRect(bounds.x(), bounds.y() + b.top(), b.left(), centerTargetHeight),
                new SourceRect(r.x(), r.y() + b.top(), b.left(), centerSourceHeight));
        add(commands, image, new TargetRect(bounds.x() + b.left(), bounds.y() + b.top(),
                centerTargetWidth, centerTargetHeight), new SourceRect(r.x() + b.left(), r.y() + b.top(),
                centerSourceWidth, centerSourceHeight));
        add(commands, image, new TargetRect(rightX, bounds.y() + b.top(), b.right(), centerTargetHeight),
                new SourceRect(rightSourceX, r.y() + b.top(), b.right(), centerSourceHeight));
        add(commands, image, new TargetRect(bounds.x(), bottomY, b.left(), b.bottom()),
                new SourceRect(r.x(), bottomSourceY, b.left(), b.bottom()));
        add(commands, image, new TargetRect(bounds.x() + b.left(), bottomY, centerTargetWidth, b.bottom()),
                new SourceRect(r.x() + b.left(), bottomSourceY, centerSourceWidth, b.bottom()));
        add(commands, image, new TargetRect(rightX, bottomY, b.right(), b.bottom()),
                new SourceRect(rightSourceX, bottomSourceY, b.right(), b.bottom()));
    }

    static void add(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            TargetRect target,
            SourceRect source
    ) {
        if (target.width() <= 0 || target.height() <= 0 || source.width() <= 0 || source.height() <= 0) {
            return;
        }
        UiTextureRegion region = image.region();
        commands.add(new UiImageDrawCommand(image.texture(), image.mode(),
                target.x(), target.y(), target.width(), target.height(),
                source.x(), source.y(), source.width(), source.height(),
                region.fullTextureWidth(), region.fullTextureHeight()));
    }

    record TargetRect(int x, int y, int width, int height) {
    }

    record SourceRect(int x, int y, int width, int height) {
    }
}
