# Trigger Wand: быстрый старт

Echo Debug — development-only мод. Если он установлен, Trigger Wand tools доступны.

## Получить wand

В игре нужен permission level 2:

```text
/echo_debug give_trigger_wand [player]
```

Item ID:

```text
echo_debug:trigger_wand
```

## Базовый поток

1. Возьми Trigger Wand в main hand или offhand.
2. ЛКМ по блоку добавляет следующую точку.
3. ПКМ отменяет последнюю точку.
4. Shift + ПКМ очищает выбор.
5. V меняет форму: Box, Sphere, Cylinder, Polygon.
6. Enter завершает selection только когда точек достаточно.
7. В экране создания укажи namespace и `trigger name/path`.
8. Сервер генерирует `.trigger.es`, валидирует его через Echo Trigger public authoring API,
   атомарно пишет файл и запускает reload.

SUCCESS появляется только после reload и проверки, что ожидаемый `TriggerId` есть в catalog.

## Где появляется файл

Canonical root:

```text
<gameDir>/story/triggers/
```

Для `jdh:chapter_1/house_exit` итоговый файл:

```text
<gameDir>/story/triggers/jdh/chapter_1/house_exit.trigger.es
```

Client никогда не пишет файл напрямую.
