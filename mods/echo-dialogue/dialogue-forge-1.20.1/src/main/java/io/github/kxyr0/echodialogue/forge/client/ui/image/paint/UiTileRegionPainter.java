package io.github.kxyr0.echodialogue.forge.client.ui.image.paint;

import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageBounds;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDefinition;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiImageDrawCommand;
import io.github.kxyr0.echodialogue.forge.client.ui.image.UiTextureRegion;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTileSettings;
import java.util.List;

/**
 * Repeats one source region for tiled UI image painting.
 */
final class UiTileRegionPainter {
    private UiTileRegionPainter() {
    }

    static void append(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds,
            UiTextureRegion source
    ) {
        if (source.width() <= 0 || source.height() <= 0 || bounds.width() <= 0 || bounds.height() <= 0) {
            return;
        }
        UiTileSettings settings = image.tileSettings();
        int tileWidth = Math.max(1, Math.round(source.width() * settings.scaleX()));
        int tileHeight = Math.max(1, Math.round(source.height() * settings.scaleY()));
        TileGrid grid = tileGrid(bounds, tileWidth, tileHeight, settings);
        requireTileLimit(grid);
        TilePattern pattern = new TilePattern(source, tileWidth, tileHeight, grid);
        if (settings.fullOnly()) {
            appendFullTiles(commands, image, bounds, pattern);
            return;
        }
        appendClippedTiles(commands, image, bounds, pattern);
    }

    private static void appendFullTiles(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds,
            TilePattern pattern
    ) {
        for (int y = pattern.grid().startY(); y + pattern.tileHeight() <= bounds.bottom(); y += pattern.tileHeight()) {
            if (y < bounds.y()) {
                continue;
            }
            appendFullTileRow(commands, image, bounds, pattern, y);
        }
    }

    private static void appendFullTileRow(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds,
            TilePattern pattern,
            int y
    ) {
        for (int x = pattern.grid().startX(); x + pattern.tileWidth() <= bounds.right(); x += pattern.tileWidth()) {
            if (x >= bounds.x()) {
                UiSlicedImagePainter.add(commands, image, target(x, y, pattern.tileWidth(), pattern.tileHeight()),
                        source(pattern.source().x(), pattern.source().y(),
                                pattern.source().width(), pattern.source().height()));
            }
        }
    }

    private static void appendClippedTiles(
            List<UiImageDrawCommand> commands,
            UiImageDefinition image,
            UiImageBounds bounds,
            TilePattern pattern
    ) {
        for (int y = pattern.grid().startY(); y < bounds.bottom(); y += pattern.tileHeight()) {
            RowClip row = rowClip(bounds, pattern, y);
            for (int x = pattern.grid().startX(); x < bounds.right(); x += pattern.tileWidth()) {
                ColumnClip column = columnClip(bounds, pattern, x);
                UiSlicedImagePainter.add(commands, image,
                        target(column.targetX(), row.targetY(), column.targetWidth(), row.targetHeight()),
                        source(column.source().start(), row.source().start(),
                                column.source().span(), row.source().span()));
            }
        }
    }

    private static RowClip rowClip(UiImageBounds bounds, TilePattern pattern, int y) {
        int targetY = Math.max(y, bounds.y());
        int targetHeight = Math.min(y + pattern.tileHeight(), bounds.bottom()) - targetY;
        return new RowClip(targetY, targetHeight, sourceClip(pattern.source().y(), pattern.source().height(),
                pattern.tileHeight(), targetY - y, targetHeight));
    }

    private static ColumnClip columnClip(UiImageBounds bounds, TilePattern pattern, int x) {
        int targetX = Math.max(x, bounds.x());
        int targetWidth = Math.min(x + pattern.tileWidth(), bounds.right()) - targetX;
        return new ColumnClip(targetX, targetWidth, sourceClip(pattern.source().x(), pattern.source().width(),
                pattern.tileWidth(), targetX - x, targetWidth));
    }

    private static SourceClip sourceClip(int sourceStart, int sourceSpan, int tileSpan, int offset, int span) {
        int start = Math.round(sourceSpan * offset / (float) tileSpan);
        int end = Math.round(sourceSpan * (offset + span) / (float) tileSpan);
        int clippedStart = Math.min(sourceSpan - 1, Math.max(0, start));
        int clippedEnd = Math.min(sourceSpan, Math.max(clippedStart + 1, end));
        return new SourceClip(sourceStart + clippedStart, clippedEnd - clippedStart);
    }

    private static TileGrid tileGrid(
            UiImageBounds bounds,
            int tileWidth,
            int tileHeight,
            UiTileSettings settings
    ) {
        int startX = start(bounds.x(), tileWidth, settings.originX());
        int startY = start(bounds.y(), tileHeight, settings.originY());
        int columns = tileCount(startX, bounds.right(), tileWidth);
        int rows = tileCount(startY, bounds.bottom(), tileHeight);
        return new TileGrid(startX, startY, columns, rows);
    }

    private static void requireTileLimit(TileGrid grid) {
        if ((long) grid.columns() * grid.rows() > UiTileSettings.MAX_TILES_PER_IMAGE) {
            throw new IllegalArgumentException("UI_IMAGE_TILE_ITERATION_LIMIT: too many tiles.");
        }
    }

    private static int start(int boundsStart, int tileSpan, int origin) {
        int offset = Math.floorMod(origin, tileSpan);
        int start = boundsStart + offset;
        return start > boundsStart ? start - tileSpan : start;
    }

    private static int tileCount(int start, int end, int span) {
        return Math.floorDiv(end - start + span - 1, span);
    }

    private static UiSlicedImagePainter.TargetRect target(int x, int y, int width, int height) {
        return new UiSlicedImagePainter.TargetRect(x, y, width, height);
    }

    private static UiSlicedImagePainter.SourceRect source(int x, int y, int width, int height) {
        return new UiSlicedImagePainter.SourceRect(x, y, width, height);
    }

    private record TilePattern(UiTextureRegion source, int tileWidth, int tileHeight, TileGrid grid) {
    }

    private record TileGrid(int startX, int startY, int columns, int rows) {
    }

    private record SourceClip(int start, int span) {
    }

    private record RowClip(int targetY, int targetHeight, SourceClip source) {
    }

    private record ColumnClip(int targetX, int targetWidth, SourceClip source) {
    }
}
