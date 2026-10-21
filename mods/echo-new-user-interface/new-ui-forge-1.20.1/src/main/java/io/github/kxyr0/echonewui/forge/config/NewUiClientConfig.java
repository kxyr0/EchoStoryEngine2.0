package io.github.kxyr0.echonewui.forge.config;

import io.github.kxyr0.echonewui.core.layout.UiLayoutConfig;
import io.github.kxyr0.echonewui.core.layout.UiReferenceCanvas;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnosticCode;
import io.github.kxyr0.echonewui.forge.diagnostic.NewUiDiagnostics;
import net.minecraftforge.common.ForgeConfigSpec;

public final class NewUiClientConfig {
    public static final ForgeConfigSpec CLIENT_SPEC;
    private static final ForgeConfigSpec.IntValue REFERENCE_WIDTH;
    private static final ForgeConfigSpec.IntValue REFERENCE_HEIGHT;
    private static final ForgeConfigSpec.IntValue REFERENCE_BOOK_WIDTH;
    private static final ForgeConfigSpec.IntValue REFERENCE_BOOK_HEIGHT;
    private static final ForgeConfigSpec.IntValue REFERENCE_SAFE_MARGIN;

    static {
        ForgeConfigSpec.Builder client = new ForgeConfigSpec.Builder();
        client.push("journal_book");
        REFERENCE_WIDTH = client.comment("Reference canvas width in physical pixels.")
                .defineInRange("reference_width", 1920, 1, 32768);
        REFERENCE_HEIGHT = client.comment("Reference canvas height in physical pixels.")
                .defineInRange("reference_height", 1080, 1, 32768);
        REFERENCE_BOOK_WIDTH = client.comment("Book width on the reference canvas.")
                .defineInRange("reference_book_width", 784, 1, 32768);
        REFERENCE_BOOK_HEIGHT = client.comment("Book height on the reference canvas.")
                .defineInRange("reference_book_height", 612, 1, 32768);
        REFERENCE_SAFE_MARGIN = client.comment("Safe margin around the book on the reference canvas.")
                .defineInRange("reference_safe_margin", 24, 0, 4096);
        client.pop();
        CLIENT_SPEC = client.build();
    }

    private NewUiClientConfig() {
    }

    public static UiLayoutConfig layout(NewUiDiagnostics diagnostics) {
        try {
            return new UiLayoutConfig(new UiReferenceCanvas(REFERENCE_WIDTH.get(), REFERENCE_HEIGHT.get()),
                    REFERENCE_BOOK_WIDTH.get(), REFERENCE_BOOK_HEIGHT.get(), REFERENCE_SAFE_MARGIN.get());
        } catch (RuntimeException exception) {
            diagnostics.warn(NewUiDiagnosticCode.NEW_UI_CONFIG_INVALID, exception.getMessage());
            return UiLayoutConfig.defaults();
        }
    }
}
