# NEW UI Client Commands

Команды регистрируются через `RegisterClientCommandsEvent` и существуют только на клиенте.

## `/echo_new_ui debug book show`

Включает preview книги.

## `/echo_new_ui debug book hide`

Выключает preview книги и возвращает HUD overlays.

## `/echo_new_ui debug book toggle`

Переключает preview.

## `/echo_new_ui debug book status`

Печатает:

- visible/hidden;
- overlay ID;
- render call counters;
- framebuffer dimensions;
- GUI dimensions;
- GUI Scale;
- physical rectangle;
- GUI rectangle;
- effective scale;
- center error;
- последнюю diagnostic строку.
