# Trigger Wand: управление

Trigger Wand активна только когда предмет находится в main hand или offhand. Если wand есть в обеих руках, main hand считается активной.

## Клавиши

- ЛКМ — поставить точку.
- ПКМ — отменить последнюю принятую точку.
- Shift + ПКМ — очистить selection.
- V — перейти к следующей форме.
- Shift + V — перейти к предыдущей форме.
- Enter — завершить selection и открыть экран создания.

V и Enter зарегистрированы как настоящие Minecraft key mappings:

- `key.echo_debug.change_shape`
- `key.echo_debug.finalize_selection`

Клавиши обрабатываются только когда Screen/chat не открыт и wand активна. Если wand не в руке, Enter не перехватывается.

## Server authority

Клиент отправляет намерение. Сервер повторно проверяет:

- `echo-debug` включён;
- permission level минимум 2;
- wand действительно в руке;
- revision selection не устарел;
- dimension совпадает;
- точка находится в допустимой дистанции.

Клиентский preview обновляется только после server ACK.
