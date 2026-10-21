# Echo Teleport Magic — полная документация DSL

Документ описывает реализованный DSL `echo_teleport_magic` для Minecraft Forge 1.20.1.
Основная команда — `TeleportMagic.To(Actor, Location)` внутри `SceneResource`.
Отдельного файла `TeleportMagicResource`, импорта или регистрации самого сервиса не требуется.

## 1. Что должно быть установлено

- Echo Teleport Magic — на сервере и клиентах.
- Echo Scene **0.4.0 или новее** — для компиляции и исполнения команды в сценах.
- Echo NPC — если телепортируется NPC; его актёр должен существовать и иметь загруженное тело в мире.
- Если установлен Echo Events, нужна версия **0.2.0 или новее**.

Для внешнего контент-пака `story/content-pack/jdh` нужен загрузчик внешнего пака Echo Story Engine.
Альтернатива — обычный datapack мира, описанный ниже.
Старый Echo Scene 0.3.0 не знает `TeleportMagic.To`: одного добавления `.es` в старую сборку недостаточно.

## 2. Где объявлять Location

Location объявляется отдельным **JSON-файлом** в серверных данных контент-пака:

```text
data/<namespace>/echo_scene/locations/<имя_места>.json
```

В твоей сборке корень внешнего JDH-пака:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh
```

Поэтому для `ChemistryDoor` создай файл:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/data/jdh/echo_scene/locations/chemistry_door.json
```

`ChemistryDoor` в прежних примерах был условным именем. Сам пример не создаёт место в мире и не объявляет JSON.
Не клади Location в `assets`, `story/configs` или прямо в `echo_teleport_magic`: каталог мест читает Echo Scene.

### Содержимое chemistry_door.json

```json
{
  "dimension": "minecraft:overworld",
  "x": 20.5,
  "y": 64.0,
  "z": 10.5
}
```

Координаты в примере замени на своё место. `y` — высота ног персонажа, а не высота головы или центра тела.
Для центра блока удобно использовать `.5` по `x` и `z`.

| Поле | Обязательно | Значение |
|---|---|---|
| `dimension` | Да | ID измерения, например `minecraft:overworld` |
| `x` | Да | Координата X, допускается дробное число |
| `y` | Да | Координата Y ног персонажа |
| `z` | Да | Координата Z, допускается дробное число |
| `id` | Нет | Явный ID Location; без него ID выводится из namespace и имени файла |

Это обычный JSON: двойные кавычки, без комментариев и запятой после последнего поля.
Дополнительные `yaw` и `pitch` этот загрузчик Location не использует. В Scene-вызове сохраняется исходное направление персонажа.

### Как получается имя ChemistryDoor

Для файла выше:

| Представление | Значение |
|---|---|
| Файл | `data/jdh/echo_scene/locations/chemistry_door.json` |
| Полный ID | `jdh:chemistry_door` |
| Символ в DSL | `ChemistryDoor` |

Echo Scene автоматически превращает части имени, разделённые `_` или `-`, в одно имя с заглавными буквами.
Например, `hall_return.json` даёт `jdh:hall_return` и `HallReturn`.
Папки внутри `locations` не добавляются к автоматически выводимому ID: используй уникальные имена файлов.

Можно указать ID явно:

```json
{
  "id": "jdh:chemistry_door",
  "dimension": "minecraft:overworld",
  "x": 20.5,
  "y": 64.0,
  "z": 10.5
}
```

При наличии `id` символ получается из этого ID. Не создавай два разных места с одним ID.
Если в разных namespace одинаковые короткие имена, в команде используй полные строковые ID.

## 3. Где хранить сцену

Сцену храни в том же серверном паке:

```text
data/jdh/echo_scene/scenes/chemistry_departure.scene.es
```

В твоей сборке это файл:

```text
C:/Users/kxyr0/AppData/Roaming/PrismLauncher/instances/Форма Мысли/minecraft/story/content-pack/jdh/data/jdh/echo_scene/scenes/chemistry_departure.scene.es
```

Имя ресурса сцены выводится из namespace и имени файла: здесь `jdh:chemistry_departure`.
Имя класса внутри файла не заменяет имя ресурса. Пиши файлы в UTF-8.

## 4. Телепортация игрока — полный пример

Содержимое `chemistry_departure.scene.es`:

```csharp
class ChemistryDeparture : SceneResource
{
    Scene Start()
    {
        TeleportMagic.To(John, ChemistryDoor);
    }
}
```

`John` — встроенный символ игрока в Echo Scene. Это **игрок, которому запущена сцена**, а не поиск человека с ником John.
Объявлять отдельный NPC John для этого не нужно.
`Player`, `@s` и произвольный ник не заменяют `John` в таком вызове.

## 5. Телепортация NPC

Если в каталоге объявлен NPC `Nolan` и его актёр уже находится в мире:

```csharp
class NolanDeparture : SceneResource
{
    Scene Start()
    {
        TeleportMagic.To(Nolan, ChemistryDoor);
    }
}
```

Для отдельного ресурса сохрани этот пример как `data/jdh/echo_scene/scenes/nolan_departure.scene.es`.
NPC-символ берётся из каталога Echo NPC; обычная подпись над сущностью сама по себе не объявляет символ.
Команда не создаёт NPC, не выбирает ближайшую сущность и не заменяет отсутствующего NPC игроком.

Сидящий персонаж сохраняет посадку во время накопления. В момент переноса он освобождает сиденье и появляется в Location.
Новая посадка в месте назначения автоматически не назначается.

## 6. Полная сигнатура и варианты аргументов

```text
TeleportMagic.To(Actor, Location);
```

| Аргумент | Что принимает |
|---|---|
| `Actor` | Символ игрока `John`, символ существующего NPC или строковый полный ID такого символа |
| `Location` | Символ объявленного Location или строковый полный ID Location |

Разрешены короткие символы:

```csharp
TeleportMagic.To(Nolan, ChemistryDoor);
```

Разрешены полные ID, если они уже есть в каталоге:

```csharp
TeleportMagic.To("jdh:nolan", "jdh:chemistry_door");
```

Для встроенного игрока полный ID — `jdh:john`:

```csharp
TeleportMagic.To("jdh:john", "jdh:chemistry_door");
```

Допустимо смешивать формы:

```csharp
TeleportMagic.To(John, "jdh:chemistry_door");
```

Кавычки означают **полный ID**, а не короткое имя. Поэтому `"ChemistryDoor"` не равнозначно `ChemistryDoor`.
Строковый ID тоже проверяется по каталогу; строка не позволяет обойти объявление ресурса.

Команда принимает ровно два аргумента. Следующие варианты не реализованы:

```text
TeleportMagic.To(John, 20, 64, 10);
TeleportMagic.To(John, ChemistryDoor, 3.0);
TeleportMagic.To(@s, ChemistryDoor);
TeleportMagic.Vanish(John);
TeleportMagic.Appear(John, ChemistryDoor);
TeleportMagic.ToXYZ(John, 20, 64, 10);
```

Настройка количества, размера, цвета, радиуса и времени отдельными аргументами или цепочками `.Duration(...)` / `.Color(...)` отсутствует.
Сейчас используется общий настроенный профиль эффекта.

## 7. Последовательность и одновременное выполнение

`To` — блокирующая команда: следующий шаг сцены начинается после переноса **и завершения остаточного эффекта**.
Не нужны `await`, ручной таймер или отдельная команда ожидания.

Для второго места создай `data/jdh/echo_scene/locations/hall_return.json`:

```json
{
  "dimension": "minecraft:overworld",
  "x": 25.5,
  "y": 64.0,
  "z": 10.5
}
```

Пример двух последовательных переносов:

```csharp
class ReturnTrip : SceneResource
{
    Scene Start()
    {
        TeleportMagic.To(John, ChemistryDoor);
        TeleportMagic.To(John, HallReturn);
    }
}
```

Пример одновременной телепортации разных персонажей через стандартный блок Echo Scene:

```csharp
class GroupDeparture : SceneResource
{
    Scene Start()
    {
        Parallel()
        {
            TeleportMagic.To(John, ChemistryDoor);
            TeleportMagic.To(Nolan, HallReturn);
        }
    }
}
```

После `Parallel` сцена ждёт обе ветви. Используй разные свободные места, чтобы персонажи не появились друг в друге.
Для одного персонажа не запускай два переноса одновременно: повторный независимый запрос отклоняется как занятость субъекта.

## 8. Как загрузить и запустить

1. Сохрани JSON Location и `.scene.es` внутри подключённого серверного контент-пака.
2. В игре выполни `/reload`, чтобы заново загрузить места и скомпилировать сцены.
3. Проверь ресурс и запусти его от игрока:

```text
/echo_scene list
/echo_scene inspect "jdh:chemistry_departure"
/echo_scene play "jdh:chemistry_departure"
```

Для этих административных команд требуется уровень прав 2. `play` привязывает `John` к запускающему игроку;
для запуска из командного блока используй стандартный Minecraft `execute as <игрок> run ...`, чтобы у команды был игрок-исполнитель.

Посмотреть активные сцены и отменить нужную:

```text
/echo_scene running
/echo_scene cancel "<scene-execution-id>"
```

ID сцены и ID отдельной телепортации — разные идентификаторы. Не подставляй один вместо другого.
Если внешний pack помечен `.archived`, он не загружается; убери маркер и перезапусти игру согласно механизму внешнего пака.

### Если используется обычный datapack мира

Размести ресурсы так:

```text
<папка мира>/datapacks/teleport_examples/
    pack.mcmeta
    data/jdh/echo_scene/locations/chemistry_door.json
    data/jdh/echo_scene/scenes/chemistry_departure.scene.es
```

`pack.mcmeta` для Minecraft 1.20.1:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Teleport scene examples"
  }
}
```

В одиночной игре папки миров находятся в `minecraft/saves`.
После сохранения выполни `/reload` и проверь `/datapack list`. Если папка отключена, включи её через `/datapack enable "file/teleport_examples"`.
Во внешнем JDH-паке `pack.mcmeta` уже есть: второй файл метаданных для одной сцены не нужен.

## 9. Как проходит эффект

Отсчёт ниже — от первых частиц; короткая сетевая подготовка до их появления может добавить небольшую задержку.

| Момент | Поведение |
|---|---|
| Начало | Частицы появляются вокруг персонажа, по высоте тела и над головой |
| Накопление | Жёлто-золотые частицы со светлыми центрами кружатся с зазором от тела и собираются в высокий столб |
| Примерно 2,35 с | Сервер переносит персонажа; происходит усиленный выброс энергии наружу |
| После переноса | Частицы остаются на исходном месте, разлетаются, замедляются и постепенно исчезают |
| Примерно 4,4 с | Исходный эффект заканчивается; блокирующая команда сцены завершается |

В месте назначения есть отдельный короткий эффект появления.
Частицы не стягиваются в тело; текущий профиль — `echo:golden_reference`, revision 2.
При задержке подтверждения переноса сервером накопление может продлиться: выброс ждёт commit и не сообщает об успехе заранее.

## 10. Ограничения и отмена

- Location должен находиться **в том же измерении**, что и персонаж. Перенос между измерениями пока не реализован.
- Место должно быть в допустимых границах мира и свободно для тела персонажа. Чанк подготавливается до старта эффекта.
- Субъект должен быть живым и доступным. Спящий персонаж и сущность, перевозящая пассажиров, не поддерживаются.
- Здоровье, инвентарь и экипировка не обнуляются. Перед переносом сохраняются текущая поза и анимация.
- Отмена до commit прекращает эффект без переноса. Отмена после commit не возвращает персонажа назад.
- Если Echo NPC уже принял необратимый перенос, отмена ожидает его подтверждения; отката в исходную точку нет.
- Восстановление того же исполнения не должно повторять уже подтверждённый перенос. Неоднозначная запись после сбоя блокирует автоматический повтор.

Сам `TeleportMagic` не меняет камеру. Управление возможностями игрока и камерой остаётся у Echo Scene.
Команда зарегистрирована в **Scene DSL**; прямого вызова из тела `DialogueResource`, `StoryResource` или `BuildNpc` этот мод не добавляет.
Из других систем запускай объявленную сцену через уже поддерживаемый ими механизм Scene.

## 11. Административные команды мода

Это команды чата/консоли Minecraft, **не дополнительные команды Scene DSL**. Нужен уровень прав 2.

| Команда | Назначение |
|---|---|
| `/echo_teleport_magic status` | Активные исполнения телепортации и их UUID |
| `/echo_teleport_magic preview <entity>` | Только эффект без реального перемещения |
| `/echo_teleport_magic teleport <entity> "namespace:location"` | Реальный перенос в именованный Location |
| `/echo_teleport_magic teleport_xyz <entity> <x> <y> <z>` | Реальный перенос в координаты текущего измерения источника команды |
| `/echo_teleport_magic cancel <teleport-execution-uuid>` | Отмена конкретного исполнения телепортации |
| `/echo_teleport_magic debug on` | Включить диагностический экран у вызывающего игрока |
| `/echo_teleport_magic debug off` | Выключить экран и снять заморозку preview |
| `/echo_teleport_magic debug freeze <seconds>` | Заморозить уже активный preview на визуальном времени от 0 до 4 |

`entity` — одна живая сущность: например `@s`, UUID или селектор с `limit=1`.
Именованная административная команда использует общий Location provider через Echo Audio; для неё нужен установленный Echo Audio и provider мест Echo Scene.
Обычный Scene-вызов `TeleportMagic.To` не требует Echo Audio.
`teleport_xyz` — диагностическая команда; её существование не означает поддержку координат в DSL.

Примеры:

```text
/echo_teleport_magic preview @s
/echo_teleport_magic teleport @s "jdh:chemistry_door"
/echo_teleport_magic teleport_xyz @s 20.5 64 10.5
/echo_teleport_magic debug on
/echo_teleport_magic debug freeze 1.95
/echo_teleport_magic debug off
```

`preview` не меняет положение и посадку. На пике preview отрисовка субъекта временно скрывается, затем восстанавливается.
Настоящий gameplay-перенос команда `debug freeze` не замораживает.
Время freeze — эталонные **визуальные** часы профиля: 1,95 соответствует реальному моменту commit через 2,35 с.
UUID телепортации выводится при запуске административной команды; для отмены сценового переноса предпочтительна отмена всей сцены.

## 12. События Echo Events

При установленном Echo Events мод публикует факты жизненного цикла:

| Предикат | Когда происходит |
|---|---|
| `TeleportMagic.Started` | Принято исполнение и начато отправление |
| `TeleportMagic.Committed` | Подтверждён реальный перенос |
| `TeleportMagic.Completed` | Завершилось исполнение и хвост эффекта |
| `TeleportMagic.Cancelled` | Принято завершение отменой |
| `TeleportMagic.Failed` | Запущенное исполнение завершилось ошибкой |

Это предикаты событий, а не методы, которые нужно писать вместо `To`.
Источник использует resource ID профиля `echo:golden_reference`, payload `subject` содержит логический ID актёра;
также сохраняются ID исполнения телепортации и корневой владелец сцены.
Preview не публикует эти gameplay-события. Ошибка проверки до принятия исполнения не обязана создавать `Started` или `Failed`.
Синтаксис подписок и реакций описан в [документации Echo Events](../../../echo-events/docs/EE1_AUTHORING.md).
Для обычной последовательной сцены подписка не нужна: `To` сам ждёт завершения.

## 13. Ошибки и способы исправить

| Симптом / сообщение | Что проверить |
|---|---|
| `SCENE_SYMBOL_NOT_FOUND` для Location | JSON лежит в `data/<namespace>/echo_scene/locations`, имя и namespace совпадают, выполнен `/reload` |
| `SCENE_SYMBOL_AMBIGUOUS` | Несколько ресурсов имеют одинаковое короткое имя; используй строковый полный ID |
| `SCENE_COMMAND_FAILED` / неизвестный `TeleportMagic` | Echo Scene обновлён, написано `TeleportMagic.To`, ровно два аргумента правильных типов |
| `SCENE_LOCATION_INVALID` | JSON корректен, есть `dimension`, `x`, `y`, `z`, числовые поля записаны числами |
| `Scene is missing or invalid` | Имя ресурса получено из файла, а не класса; смотри `/echo_scene list` и `inspect` |
| NPC не найден / тело недоступно | NPC объявлен в каталоге и уже материализован в текущем мире |
| `TELEPORT_CROSS_DIMENSION_UNSUPPORTED` | `dimension` Location совпадает с измерением субъекта |
| `Destination obstructed` | В месте назначения хватает свободного пространства для тела |
| `Destination outside world bounds` | Координаты внутри высоты строительства и границы мира |
| `TELEPORT_SUBJECT_BUSY` | Для персонажа уже идёт другой перенос; дождись завершения или отмени его |
| `Named Location provider unavailable` | Для административного именованного переноса доступен Echo Audio; для диагностики можно использовать `teleport_xyz` |
| `TELEPORT_RECOVERY_AMBIGUOUS` | Осталась неоднозначная запись необратимого commit; сначала проверь фактическое положение персонажа, не запускай повтор вслепую |

## 14. Краткая памятка

1. Объяви место JSON-файлом `data/jdh/echo_scene/locations/chemistry_door.json`.
2. Добавь сцену `.scene.es` в `data/jdh/echo_scene/scenes`.
3. Внутри `Scene Start()` напиши `TeleportMagic.To(John, ChemistryDoor);`.
4. Выполни `/reload`, затем `inspect` и `play` для ID сцены.
5. Для NPC замени `John` на существующий символ NPC; Location можно оставить тем же.
