# Echo New User Interface

`echo-new-user-interface` - независимый клиентский Forge 1.20.1 мод для первого milestone NUI1.

Мод по умолчанию ничего не показывает. Preview открытой книги включается только клиентской командой и рисуется как один цельный PNG в центре физического framebuffer. Существующий Echo Dialogue, его UI image renderer, NineSliceRenderer и debug 9-slice screen не используются и не изменяются.

## Модули

- `new-ui-core` - чистый Java layout: reference canvas, viewport, FIT scale, physical rectangle, GUI mapping и unit tests.
- `new-ui-forge-1.20.1` - Forge entrypoint, client-only command, overlay, HUD suppression, texture blit, config и diagnostics.

Development-only `new-ui-testmod-1.20.1` не создан в этом milestone: текущие проверки покрыты pure unit tests и командой `status`, а production JAR собирается только из Forge-модуля с embedded `new-ui-core`.

## Команды

- `/echo_new_ui debug book show`
- `/echo_new_ui debug book hide`
- `/echo_new_ui debug book toggle`
- `/echo_new_ui debug book status`

## Texture

Путь:

`new-ui-forge-1.20.1/src/main/resources/assets/echo_new_user_interface/textures/gui/debug/journal_book.png`

ResourceLocation:

`echo_new_user_interface:textures/gui/debug/journal_book.png`

Source size: `196 x 153`.

## Сборка

```powershell
.\gradlew.bat test --console=plain
.\gradlew.bat check --console=plain
```
