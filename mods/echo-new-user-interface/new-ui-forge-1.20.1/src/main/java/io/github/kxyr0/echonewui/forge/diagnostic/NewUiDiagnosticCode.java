package io.github.kxyr0.echonewui.forge.diagnostic;

public enum NewUiDiagnosticCode {
    NEW_UI_VIEWPORT_INVALID("Некорректный снимок viewport."),
    NEW_UI_FRAMEBUFFER_INVALID("Некорректный физический framebuffer."),
    NEW_UI_GUI_VIEWPORT_INVALID("Некорректный GUI viewport."),
    NEW_UI_REFERENCE_CANVAS_INVALID("Некорректный reference canvas."),
    NEW_UI_BOOK_SIZE_INVALID("Некорректный размер книги."),
    NEW_UI_LAYOUT_SCALE_INVALID("Некорректный scale layout."),
    NEW_UI_LAYOUT_RECT_INVALID("Некорректный rectangle книги."),
    NEW_UI_TEXTURE_MISSING("Texture книги не найдена."),
    NEW_UI_RENDER_FAILED("Ошибка render книги."),
    NEW_UI_RENDER_STATE_RESTORE_FAILED("Не удалось восстановить render state."),
    NEW_UI_CONFIG_INVALID("Некорректный client config."),
    NEW_UI_CLIENT_COMMAND_REGISTRATION_FAILED("Ошибка регистрации client command."),
    NEW_UI_HUD_SUPPRESSION_FAILED("Ошибка подавления HUD.");

    private final String russianMessage;

    NewUiDiagnosticCode(String russianMessage) {
        this.russianMessage = russianMessage;
    }

    public String russianMessage() {
        return russianMessage;
    }
}
