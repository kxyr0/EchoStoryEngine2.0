package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiSliceBorders;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import java.util.ArrayList;
import java.util.List;

/**
 * Repeats source regions while preserving optional sliced borders.
 */
public final class UiTiledImagePainter {
    private UiTiledImagePainter() {
    }

    public static List<UiImageDrawCommand> commands(UiImageDefinition image, UiImageBounds bounds) {
        UiSliceBorders border = image.borders();
        if (border.empty()) {
            return tiledRegion(image, bounds, image.region());
        }
        UiSlicedImagePainter.requireBordersFit(border, bounds);
        List<UiImageDrawCommand> commands = new ArrayList<>();
        appendBordered(commands, image, bounds);
        return List.copyOf(commands);
    }

    private static void appendBordered(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds
    ) {
        UiSliceBorders b = image.borders();
        UiTextureRegion r = image.region();
        int centerTargetWidth = bounds.width() - b.minimumWidth();
        int centerTargetHeight = bounds.height() - b.minimumHeight();
        int centerSourceWidth = r.width() - b.minimumWidth();
        int centerSourceHeight = r.height() - b.minimumHeight();
        int rightX = bounds.right() - b.right();
        int bottomY = bounds.bottom() - b.bottom();
        int rightSourceX = r.right() - b.right();
        int bottomSourceY = r.bottom() - b.bottom();
        UiSlicedImagePainter.add(commands, image, target(bounds.x(), bounds.y(), b.left(), b.top()),
                source(r.x(), r.y(), b.left(), b.top()));
        appendTiled(commands, image, target(bounds.x() + b.left(), bounds.y(), centerTargetWidth, b.top()),
                source(r.x() + b.left(), r.y(), centerSourceWidth, b.top()));
        UiSlicedImagePainter.add(commands, image, target(rightX, bounds.y(), b.right(), b.top()),
                source(rightSourceX, r.y(), b.right(), b.top()));
        appendTiled(commands, image, target(bounds.x(), bounds.y() + b.top(), b.left(), centerTargetHeight),
                source(r.x(), r.y() + b.top(), b.left(), centerSourceHeight));
        appendTiled(commands, image, target(bounds.x() + b.left(), bounds.y() + b.top(),
                centerTargetWidth, centerTargetHeight), source(r.x() + b.left(), r.y() + b.top(),
                centerSourceWidth, centerSourceHeight));
        appendTiled(commands, image, target(rightX, bounds.y() + b.top(), b.right(), centerTargetHeight),
                source(rightSourceX, r.y() + b.top(), b.right(), centerSourceHeight));
        UiSlicedImagePainter.add(commands, image, target(bounds.x(), bottomY, b.left(), b.bottom()),
                source(r.x(), bottomSourceY, b.left(), b.bottom()));
        appendTiled(commands, image, target(bounds.x() + b.left(), bottomY, centerTargetWidth, b.bottom()),
                source(r.x() + b.left(), bottomSourceY, centerSourceWidth, b.bottom()));
        UiSlicedImagePainter.add(commands, image, target(rightX, bottomY, b.right(), b.bottom()),
                source(rightSourceX, bottomSourceY, b.right(), b.bottom()));
    }

    private static List<UiImageDrawCommand> tiledRegion(
            UiImageDefinition image,
            UiImageBounds bounds,
            UiTextureRegion region
    ) {
        List<UiImageDrawCommand> commands = new ArrayList<>();
        appendTiled(commands, image, bounds, region);
        return List.copyOf(commands);
    }

    private static void appendTiled(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiSlicedImagePainter.TargetRect target,
            UiSlicedImagePainter.SourceRect source
    ) {
        if (target.width() <= 0 || target.height() <= 0 || source.width() <= 0 || source.height() <= 0) {
            return;
        }
        UiTextureRegion region = image.region();
        appendTiled(commands, image, new UiImageBounds(target.x(), target.y(), target.width(), target.height()),
                new UiTextureRegion(source.x(), source.y(), source.width(), source.height(),
                        region.fullTextureWidth(), region.fullTextureHeight()));
    }

    private static void appendTiled(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds,
            UiTextureRegion source
    ) {
        UiTileRegionPainter.append(commands, image, bounds, source);
    }

    private static UiSlicedImagePainter.TargetRect target(int x, int y, int width, int height) {
        return new UiSlicedImagePainter.TargetRect(x, y, width, height);
    }

    private static UiSlicedImagePainter.SourceRect source(int x, int y, int width, int height) {
        return new UiSlicedImagePainter.SourceRect(x, y, width, height);
    }

}
