package io.github.kxyr0.echotimehud.forge.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.kxyr0.echotimehud.api.HudAnchor;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HudConfigBinderTest {
    private final HudConfigBinder binder = new HudConfigBinder();

    @Test void bindsAuthoritativeResource() {
        HudConfigResult result = binder.bind(valid("HH:mm"), "hud.es", 7);
        assertTrue(result.config().isPresent());
        assertEquals(HudAnchor.TOP_RIGHT, result.config().orElseThrow().anchor());
        assertEquals(-24, result.config().orElseThrow().offsetX());
        assertEquals(1920, result.config().orElseThrow().referenceWidth());
        assertEquals(7, result.config().orElseThrow().revision());
    }

    @Test void rejectsUnsupportedFormatWithStableCode() {
        HudConfigResult result = binder.bind(valid("H:mm"), "hud.es", 8);
        assertTrue(result.config().isEmpty());
        assertEquals("HUD_CONFIG_FORMAT_INVALID", result.diagnostics().get(0).code());
    }

    @Test void loaderRetainsLastKnownGood(@TempDir Path gameDirectory) throws Exception {
        Path path = gameDirectory.resolve(HudConfigLoader.RELATIVE_PATH);
        Files.createDirectories(path.getParent());
        Files.writeString(path, valid("HH:mm"));
        HudConfigLoader loader = new HudConfigLoader(gameDirectory);
        assertTrue(loader.reload());
        long revision = loader.current().revision();
        Files.writeString(path, valid("H:mm"));
        assertFalse(loader.reload());
        assertEquals(revision, loader.current().revision());
        assertEquals("HH:mm", loader.current().format());
    }

    private String valid(String format) {
        return """
                namespace JDH.Config.TimeHud;
                class MainTimeHud : TimeHudConfigResource {
                    private void Init() {
                        Id("jdh:main_time_hud"); Enabled(true); Anchor(TOP_RIGHT);
                        Offset(x: -24, y: 24); ReferenceCanvas(width: 1920, height: 1080);
                        SafeMargin(12); TextScale(1.0); Format("%s"); ShowDay(false);
                        Shadow(true); Background(false); Opacity(1.0); RespectHideHud(true);
                        ClampToScreen(true); ScreenPolicy(SHOW_DURING_GAME_SCREENS);
                    }
                }
                """.formatted(format);
    }
}

