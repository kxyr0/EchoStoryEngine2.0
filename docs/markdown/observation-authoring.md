# Echo Observation Authoring

`echo_observation_authoring` — локальный инструмент расстановки Observation прямо в одиночной игре. Он редактирует исходные `.es`, просит Observation выполнить hot reload и показывает подтверждённый сервером результат. Мод предназначен для разработки контента; в релизной сборке для игроков его можно удалить.

## Требования и область работы

- Нужен `echo_observation` совместимой версии.
- Работает только в integrated server, то есть в одиночном мире.
- Редактирует только файлы внутри `story/content-pack/jdh/data/<namespace>/echo_observation/observations/`.
- Параметр `enabled` находится в `config/echo-observation-authoring-client.toml` и по умолчанию равен `true`.
- Максимальная дистанция луча постановки — 64 блока.

На dedicated server инструмент не активируется и сообщает `authoring requires an integrated server`.

## Управление

| Клавиша | Действие |
|---|---|
| `F6` | включить или выключить authoring |
| `Insert` | поставить выбранную Observation в точку raycast |
| `Delete` | удалить `At(...)` у выбранной Observation |
| `[` / `]` | предыдущая / следующая Observation |
| `Shift` + `[` / `]` | предыдущий / следующий resource |
| `Tab` | открыть picker всех declarations |

Все клавиши можно переназначить в обычных настройках управления Minecraft.

## Правильная последовательность постановки

1. Откройте одиночный мир и нажмите `F6`.
2. Нажмите `Tab`, начните вводить resource ID, local ID или полный ID точки.
3. Выберите строку стрелками и нажмите `Enter`; двойной щелчок мышью тоже подтверждает выбор. `Esc` закрывает picker, не меняя выбор.
4. Наведитесь на конкретную грань блока. HUD должен показывать `hit X Y Z / FACE`, а не `raycast miss`.
5. Нажмите `Insert` один раз и дождитесь `Reload confirmed, catalog r...`.
6. После успешной постановки инструмент автоматически выбирает следующую нерасставленную declaration в том же resource.

Когда нерасставленных declarations больше нет, выводится `ALL OBSERVATIONS PLACED.`.

## Что именно записывает `Insert`

Raycast фиксирует мировые данные в момент нажатия:

- dimension;
- координаты блока;
- грань блока;
- локальный `Offset` внутри блока.

Результат записывается как обычный DSL:

```echoscript
At(
    Block(2, -2, 415),
    Offset(0.000, 0.552, 0.360),
    face: WEST,
    dimension: "minecraft:overworld"
);
```

После записи точка живёт в world space. Поворот камеры и движение игрока не меняют её координаты. Цветные authoring-маркеры также рендерятся в world space: один маркер показывает выбранную сохранённую точку, другой — текущий raycast preview.

## Безопасность исходника

Инструмент не переписывает весь класс по собственному шаблону. Он находит declaration по source metadata и заменяет только точный диапазон `At(...)` либо вставляет новый вызов. Перед сохранением candidate повторно проходит parser/binder Observation. Запись выполняется через временный файл и атомарную замену.

Если путь не принадлежит разрешённой директории, исходник отсутствует, raycast промахнулся или candidate невалиден, файл не изменяется. До получения reload acknowledgement повторные `Insert`/`Delete` блокируются.

## Picker

Picker содержит все загруженные Observation declarations, в том числе уже расставленные. Поиск сопоставляет введённый текст с полным ID, resource ID и local ID. Текущий выбор подсвечивается; прокрутка сохраняет выбранную строку в видимой области.

Выбранный resource автоматически активируется для локального игрока, чтобы поставленную точку можно было сразу проверить в концентрации.

## Диагностика

Полезные команды основного мода:

```text
/echo_observation status
/echo_observation list
/echo_observation inspect "jdh:museum_room#broken_window"
/echo_observation reload
```

Если `Insert` ничего не делает, проверьте верхний authoring HUD:

- `F6: authoring disabled` — включите `F6`;
- `raycast miss` — наведитесь на блок в пределах 64 блоков;
- `No Observation declarations` — catalog не содержит declarations;
- `Waiting for reload confirmation` — дождитесь ответа сервера;
- `Source file is not editable` — source лежит вне разрешённого content-pack;
- `Reload rejected: ...` — исправьте указанную DSL-ошибку; runtime продолжит использовать last-known-good catalog.

## Удаление из production

Удаление `echo_observation_authoring` не удаляет расставленные точки: координаты уже сохранены в `.es`. В production остаются `echo_observation`, content-pack и нужные интеграционные моды.
