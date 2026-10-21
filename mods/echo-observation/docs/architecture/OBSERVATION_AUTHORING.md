# Observation authoring

Authoring работает только в integrated server и только если `echo-observation-authoring-client.toml: enabled=true`.

1. Создайте `Observation` без `At(...)` в `<instance>/minecraft/story/content-pack/jdh/data/<namespace>/echo_observation/observations/*.es` и сохраните файл.
2. Выполните resource reload, войдите в мир и нажмите F6.
3. `[` / `]` выбирают Observation; `Shift+[` / `Shift+]` переключают resource. Первой выбирается unplaced declaration. `Tab` открывает searchable picker: resources идут лексикографически, внутри ресурса сначала `UNPLACED`, затем `PLACED`, затем ID. Поиск нечувствителен к регистру и проверяет ResourceId, ObservationId, имя declaration и source path. `Enter` или двойной клик подтверждают выбор; `Esc` отменяет его, не меняя текущую точку.
4. Посмотрите active camera на поверхность в пределах 64 blocks. Overlay явно показывает Resource, Selected declaration, ID, status, `PLACED/UNPLACED`, raycast и подсказки `Insert`/`Tab`.
5. Нажмите Insert. Мод вычислит block-local Offset и face, изменит только `At` span выбранной declaration, повторно прогонит реальный parser/binder, выполнит UTF-8 atomic replace и запросит server resource reload.
6. Дождитесь `Reload confirmed`. После placement выбирается следующая unplaced point только в том же resource. Если их больше нет, выбор остаётся на текущей точке и status показывает `ALL OBSERVATIONS PLACED.`. В мире остаются квадрат anchor, крест и normal; выбранная point выделена, raycast preview — розовый.
7. Для reposition снова наведитесь и нажмите Insert. Старый `At` заменяется, второй не добавляется. Delete удаляет только `At`, возвращая point в `UNPLACED`.
8. F6 выключает authoring.

При raycast miss, неизвестном source path, invalid candidate или write failure исходник не меняется. Редактор не делает regex/whole-file rewrite и не читает filesystem на каждом tick: чтение происходит только по Insert/Delete.
