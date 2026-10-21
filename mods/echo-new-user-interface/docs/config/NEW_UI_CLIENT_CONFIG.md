# NEW UI Client Config

Config type: Forge client config.

Path создаётся Forge автоматически для mod ID `echo_new_user_interface`.

Поля:

- `journal_book.reference_width`, default `1920`;
- `journal_book.reference_height`, default `1080`;
- `journal_book.reference_book_width`, default `784`;
- `journal_book.reference_book_height`, default `612`;
- `journal_book.reference_safe_margin`, default `24`.

Все поля валидируются через `ForgeConfigSpec.defineInRange`.

Если runtime config оказывается некорректным, `NewUiClientConfig.layout` пишет diagnostic `NEW_UI_CONFIG_INVALID` и возвращает defaults.
