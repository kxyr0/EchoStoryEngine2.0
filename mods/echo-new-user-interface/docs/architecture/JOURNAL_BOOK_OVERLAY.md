# Journal Book Overlay

Overlay ID:

`echo_new_user_interface:journal_book`

Texture:

`echo_new_user_interface:textures/gui/debug/journal_book.png`

Render operation:

- один full texture blit;
- source: `196 x 153`;
- destination: рассчитанный centered GUI rectangle;
- alpha blending включается только на время draw;
- shader color возвращается в `1,1,1,1`;
- texture filter принудительно ставится в nearest mode без сглаживания.
- на левую страницу поверх texture выводятся строки `Начать игру`, `Настройки`,
  `Выход в главное меню`.

Overlay выключен по умолчанию и рисует книгу только когда `JournalBookPreviewState.visible() == true`.
