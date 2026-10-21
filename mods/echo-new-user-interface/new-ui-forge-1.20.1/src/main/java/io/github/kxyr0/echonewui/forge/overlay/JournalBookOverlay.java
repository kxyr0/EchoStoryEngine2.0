package io.github.kxyr0.echonewui.forge.overlay;

import io.github.kxyr0.echonewui.core.layout.JournalBookLayout;
import io.github.kxyr0.echonewui.core.layout.JournalBookLayoutResult;
import io.github.kxyr0.echonewui.forge.EchoNewUserInterfaceMod;
import io.github.kxyr0.echonewui.forge.client.JournalBookPreviewState;
import io.github.kxyr0.echonewui.forge.config.NewUiClientConfig;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnosticCode;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import io.github.kxyr0.echonewui.forge.render.JournalBookMenuText;
import io.github.kxyr0.echonewui.forge.render.JournalBookRenderer;
import io.github.kxyr0.echonewui.forge.viewport.UiViewportSnapshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.gui.overlay.ForgeGui;

public final class JournalBookOverlay {
    public static final ResourceLocation TEXTURE = new ResourceLocation(EchoNewUserInterfaceMod.MOD_ID,
            "textures/gui/debug/journal_book.png");
    private final JournalBookPreviewState state;
    private final NewUiDiagnostics diagnostics;
    private final JournalBookLayout layout = new JournalBookLayout();
    private final JournalBookRenderer renderer = new JournalBookRenderer();
    private final JournalBookMenuText menuText = new JournalBookMenuText();

    public JournalBookOverlay(JournalBookPreviewState state, NewUiDiagnostics diagnostics) {
        this.state = state;
        this.diagnostics = diagnostics;
    }

    public void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        if (!state.visible()) {
            state.recordHiddenRender();
            return;
        }
        UiViewportSnapshot.capture(diagnostics).ifPresent(viewport -> renderVisible(graphics, viewport));
    }

    private void renderVisible(GuiGraphics graphics, io.github.kxyr0.echonewui.core.layout.UiViewport viewport) {
        try {
            JournalBookLayoutResult result = layout.layout(viewport, NewUiClientConfig.layout(diagnostics));
            renderer.render(graphics, TEXTURE, result.guiRect(), diagnostics);
            menuText.render(graphics, result.guiRect());
            state.recordVisibleRender(result);
        } catch (RuntimeException exception) {
            diagnostics.error(NewUiDiagnosticCode.NEW_UI_RENDER_FAILED, exception.getMessage(), exception);
        }
    }
}
