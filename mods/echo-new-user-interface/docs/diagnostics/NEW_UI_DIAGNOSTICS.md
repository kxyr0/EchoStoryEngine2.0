# NEW UI Diagnostics

Diagnostics хранят последнюю строку и логируются rate-limited.

Коды:

- `NEW_UI_VIEWPORT_INVALID`
- `NEW_UI_FRAMEBUFFER_INVALID`
- `NEW_UI_GUI_VIEWPORT_INVALID`
- `NEW_UI_REFERENCE_CANVAS_INVALID`
- `NEW_UI_BOOK_SIZE_INVALID`
- `NEW_UI_LAYOUT_SCALE_INVALID`
- `NEW_UI_LAYOUT_RECT_INVALID`
- `NEW_UI_TEXTURE_MISSING`
- `NEW_UI_RENDER_FAILED`
- `NEW_UI_RENDER_STATE_RESTORE_FAILED`
- `NEW_UI_CONFIG_INVALID`
- `NEW_UI_CLIENT_COMMAND_REGISTRATION_FAILED`
- `NEW_UI_HUD_SUPPRESSION_FAILED`

Команда `/echo_new_ui debug book status` печатает последнюю diagnostic строку.

Логи не пишутся каждый frame: одинаковый код логируется не чаще одного раза в 5 секунд.
