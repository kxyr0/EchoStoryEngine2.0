# Journal Book Texture

Source size:

`196 x 153`

Target path:

`new-ui-forge-1.20.1/src/main/resources/assets/echo_new_user_interface/textures/gui/debug/journal_book.png`

ResourceLocation:

`echo_new_user_interface:textures/gui/debug/journal_book.png`

Render type:

`full texture blit`

Nine-slice:

`не используется`

Reference canvas:

`1920 x 1080`

Reference book size:

`784 x 612`

Reference rectangle:

`568, 234, 784, 612`

Anchor:

`CENTER`

Scale mode:

`FIT`

Sampling:

`NEAREST`

Texture рисуется целиком, потому что книга является цельным изображением. Независимое растягивание X/Y запрещено: оно меняет aspect ratio `196 / 153` и деформирует разворот.

GUI Scale не является источником layout. Сначала рассчитывается physical rectangle, затем четыре края преобразуются в GUI coordinates.

Чтобы заменить PNG, положите новый файл в target path. Если source aspect ratio остаётся прежним, layout менять не нужно.
