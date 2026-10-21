package io.github.kxyr0.echodialogue.forge.client.ui.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillDirection;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiFillSettings;
import io.github.kxyr0.echodialogue.forge.client.ui.settings.UiTileSettings;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

final class UiImageRendererTest {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("echo_dialogue", "textures/gui/panel/test_dialogue_panel.png");
    private static final UiSliceBorders BORDERS = new UiSliceBorders(6, 5, 6, 8);

    @Test
    void simpleStretchesSourceRegionToDestination() {
        UiImageDefinition image = UiImageDefinition.simple(TEXTURE, 29, 28);
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(10, 20, 120, 64));

        assertEquals(1, commands.size());
        assertCommand(commands.get(0), new Expected(10, 20, 120, 64, 0, 0, 29, 28));
    }

    @Test
    void slicedPreservesCornersAndStretchesEdges() {
        UiImageDefinition image = UiImageDefinition.sliced(TEXTURE, 29, 28, BORDERS);
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(10, 20, 120, 64));

        assertEquals(9, commands.size());
        assertCommand(commands.get(0), new Expected(10, 20, 6, 6, 0, 0, 6, 6));
        assertCommand(commands.get(2), new Expected(125, 20, 5, 6, 24, 0, 5, 6));
        assertCommand(commands.get(4), new Expected(16, 26, 109, 50, 6, 6, 18, 14));
        assertCommand(commands.get(8), new Expected(125, 76, 5, 8, 24, 20, 5, 8));
    }

    @Test
    void tiledClipsPartialTilesWithoutStretching() {
        UiImageDefinition image = tiled(UiSliceBorders.none());
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(0, 0, 35, 33));

        assertEquals(4, commands.size());
        assertCommand(commands.get(0), new Expected(0, 0, 29, 28, 0, 0, 29, 28));
        assertCommand(commands.get(1), new Expected(29, 0, 6, 28, 0, 0, 6, 28));
        assertCommand(commands.get(2), new Expected(0, 28, 29, 5, 0, 0, 29, 5));
        assertCommand(commands.get(3), new Expected(29, 28, 6, 5, 0, 0, 6, 5));
    }

    @Test
    void tiledFullOnlySkipsPartialTiles() {
        UiImageDefinition image = tiled(UiSliceBorders.none(), new UiTileSettings(1.0F, 1.0F, 0, 0, true));
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(0, 0, 35, 33));

        assertEquals(1, commands.size());
        assertCommand(commands.get(0), new Expected(0, 0, 29, 28, 0, 0, 29, 28));
    }

    @Test
    void tiledKeepsBordersAndRepeatsCenter() {
        UiImageDefinition image = tiled(BORDERS);
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(0, 0, 40, 34));

        assertEquals(16, commands.size());
        assertCommand(commands.get(0), new Expected(0, 0, 6, 6, 0, 0, 6, 6));
        assertCommand(commands.get(2), new Expected(24, 0, 11, 6, 6, 0, 11, 6));
        assertCommand(commands.get(6), new Expected(6, 6, 18, 14, 6, 6, 18, 14));
        assertCommand(commands.get(9), new Expected(24, 20, 11, 6, 6, 6, 11, 6));
        assertCommand(commands.get(15), new Expected(35, 26, 5, 8, 24, 20, 5, 8));
    }

    @Test
    void tiledOriginOffsetsFullTileGrid() {
        UiImageDefinition image = new UiImageDefinition(TEXTURE, UiTextureRegion.full(10, 10),
                UiImageRenderMode.TILED, UiSliceBorders.none(), new UiTileSettings(1.0F, 1.0F, 3, 4, true),
                UiFillSettings.full(), null, null, 0);
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(0, 0, 25, 24));

        assertEquals(4, commands.size());
        assertCommand(commands.get(0), new Expected(3, 4, 10, 10, 0, 0, 10, 10));
        assertCommand(commands.get(1), new Expected(13, 4, 10, 10, 0, 0, 10, 10));
        assertCommand(commands.get(2), new Expected(3, 14, 10, 10, 0, 0, 10, 10));
        assertCommand(commands.get(3), new Expected(13, 14, 10, 10, 0, 0, 10, 10));
    }

    @Test
    void filledClipsGeometryByDirection() {
        UiImageDefinition image = new UiImageDefinition(TEXTURE, UiTextureRegion.full(100, 20),
                UiImageRenderMode.FILLED, UiSliceBorders.none(), UiTileSettings.defaults(),
                new UiFillSettings(UiFillDirection.RIGHT_TO_LEFT, 0.25F), null, null, 0);
        List<UiImageDrawCommand> commands =
                UiImageRenderer.drawCommands(image, new UiImageBounds(10, 20, 80, 16));

        assertEquals(1, commands.size());
        assertCommand(commands.get(0), new Expected(70, 20, 20, 16, 75, 0, 25, 20));
    }

    @Test
    void tileCountIsBounded() {
        assertThrows(IllegalArgumentException.class,
                () -> UiImageRenderer.drawCommands(tiled(UiSliceBorders.none()),
                        new UiImageBounds(0, 0, 2000, 2000)));
    }

    @Test
    void fillZeroProducesNoDrawCommands() {
        UiImageDefinition image = new UiImageDefinition(TEXTURE, UiTextureRegion.full(10, 10),
                UiImageRenderMode.FILLED, UiSliceBorders.none(), UiTileSettings.defaults(),
                new UiFillSettings(UiFillDirection.LEFT_TO_RIGHT, 0.0F), null, null, 0);

        assertTrue(UiImageRenderer.drawCommands(image, new UiImageBounds(0, 0, 30, 30)).isEmpty());
    }

    private static UiImageDefinition tiled(UiSliceBorders borders) {
        return tiled(borders, UiTileSettings.defaults());
    }

    private static UiImageDefinition tiled(UiSliceBorders borders, UiTileSettings settings) {
        return new UiImageDefinition(TEXTURE, UiTextureRegion.full(29, 28), UiImageRenderMode.TILED, borders,
                settings, UiFillSettings.full(), null, null, 0);
    }

    private static void assertCommand(UiImageDrawCommand command, Expected expected) {
        assertEquals(expected.x(), command.x());
        assertEquals(expected.y(), command.y());
        assertEquals(expected.targetWidth(), command.targetWidth());
        assertEquals(expected.targetHeight(), command.targetHeight());
        assertEquals(expected.sourceX(), command.sourceX());
        assertEquals(expected.sourceY(), command.sourceY());
        assertEquals(expected.sourceWidth(), command.sourceWidth());
        assertEquals(expected.sourceHeight(), command.sourceHeight());
    }

    private record Expected(
            int x,
            int y,
            int targetWidth,
            int targetHeight,
            int sourceX,
            int sourceY,
            int sourceWidth,
            int sourceHeight
    ) {
    }
}
