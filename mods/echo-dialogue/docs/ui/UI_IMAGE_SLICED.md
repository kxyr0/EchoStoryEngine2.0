# UI image SLICED

`SLICED` — текущая рабочая 9-slice семантика, перенесённая в общий `UiImageRenderer`.

Правила:

- четыре угла не растягиваются;
- верхний и нижний края растягиваются только по X;
- левый и правый края растягиваются только по Y;
- центр растягивается по X и Y.

Borders задаются как `left`, `right`, `top`, `bottom`. Destination не может быть меньше суммы противоположных borders:
иначе renderer выдаёт `UI_IMAGE_DESTINATION_TOO_SMALL`.

Для текущей paper texture рабочие borders:

- `left = 6`
- `right = 5`
- `top = 6`
- `bottom = 8`

В live Prism сейчас можно пробовать другие borders в отдельном overlay:

`story/configs/jdh/echo_dialogue/client_overlay/dialogue_panel_background.dialogue-overlay.es`
