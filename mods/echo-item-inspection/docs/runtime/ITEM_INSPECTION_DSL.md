# Осмотр предмета — EchoScript 2

Минимальное определение:

```csharp
class FatherLetterInspection : ItemInspectionResource
{
    Item(
        "jdh:father_letter"
    );
}
```

Namespace, `Build()`, `Init()`, `return` и `new ItemInspection(...)` не нужны.
`jdh:father_letter` должен существовать в Minecraft item registry или каталоге Echo Item Pack.
Этот пример не добавляет сам предмет и не создаёт production lore.

Положите файл `father_letter.inspection.es` в один из каталогов:

| Поставка | Путь |
|---|---|
| Content mod/resource | `data/jdh/echo_item_inspection/inspections/father_letter.inspection.es` |
| Client resource pack | `assets/jdh/echo_item_inspection/inspections/father_letter.inspection.es` |
| Внешний контент | `<minecraft>/story/item-inspection/jdh/father_letter.inspection.es` |

Inspection resource ID выводится из namespace контента и имени файла:
`jdh:father_letter`. Имя класса не является persistent ID. `Item(...)` — отдельная
привязка к типу предмета; viewer показывает конкретный наведённый stack с его count/NBT.

## Предметы в мире через IPLA

Тот же `ItemInspectionResource` автоматически действует для предмета, размещённого
через IPLA 6.3.1. Дополнительный ресурс или `AllowIpla` не нужен. Наведите камеру
на предмет: слева снизу появится «Осмотреть» с текущей клавишей (по умолчанию E).
Клавиша настраивается в управлении, категория «Echo — осмотр предметов»;
поддерживается также кнопка мыши. Предмет остаётся в мире, инвентарь не меняется,
Esc возвращает прямо в игру. Без подходящей цели E открывает обычный инвентарь.
Стены и дистанция ограничивают осмотр; концентрация, диалог и другие экраны имеют приоритет.
Общая дистанция задаётся `worldInspectionDistance` в `echo_item_inspection-client.toml`,
по умолчанию 4.5 блока. Указывать её в каждом определении не требуется.
Без IPLA осмотр из инвентаря продолжает работать.

## Как узнать ID предмета

В игре нажмите F3+H, затем наведите курсор на предмет в инвентаре. В расширенной
подсказке появится registry ID, например `minecraft:stone` или `jdh:father_letter`.
Именно эту строку нужно передать в `Item("...")`. F3+H повторно отключает расширенные подсказки.

Привязка использует зарегистрированный тип предмета, а не его видимое название,
имя класса inspection или имя файла модели. Переименование, CustomModelData и другие
NBT-варианты одного item ID используют то же определение; модель и данные берутся
из конкретного stack. Все предметы автоматически inspectable не становятся.

Команда `/echo_item_inspection list` показывает подключённые привязки:
`inspection-resource-id -> item-id revision=N`. Она помогает проверить, для каких
предметов уже существует рабочий осмотр. Пустой каталог означает, что определения ещё
не добавлены; одного production JAR для включения осмотра всех предметов недостаточно.

## Необязательные название и описание

```csharp
class FatherLetterInspection : ItemInspectionResource
{
    Item("jdh:father_letter");
    Title("Письмо");
    Description("Здесь можно написать свой текст о предмете.");
}
```

`Title(...)` и `Description(...)` задаются на уровне класса, рядом с `Item(...)`.
Каждый вызов необязателен и допускается один раз. Без Title viewer использует
настоящее отображаемое имя конкретного stack; без Description текст описания отсутствует.
Пустые строки также разрешены. Поддерживаются обычные строковые литералы и Unicode:
название ограничено 256, описание — 8192 символами Unicode (code points).
Неверный тип аргумента, повторное объявление или превышение длины — ошибка
`ITEM_INSPECTION_INVALID_TEXT` либо ошибка повторного объявления; LKG сохраняется.

Длинный текст переносится и доступен целиком через прокрутку панели. Текст относится
к inspection definition и не меняет имя, lore или NBT предмета в инвентаре.
Открытая сессия сохраняет свою версию названия и описания до закрытия.
На широком экране текст размещается справа от модели, на узком или вертикальном — ниже.
Прокрутка текста: ползунок справа, стрелки вверх/вниз, PgUp/PgDn, Home/End.
Колесо мыши всегда управляет zoom модели, в том числе над текстовой панелью.

## Необязательный View

```csharp
class FatherLetterInspection : ItemInspectionResource
{
    Item("jdh:father_letter");

    View()
    {
        Scale(1.15);
        Rotation(x: 12, y: -20, z: 0);
        Zoom(min: 0.65, max: 2.40, initial: 1.0);
    }
}
```

Любую настройку внутри `View()` можно опустить. Без View действуют defaults:
Scale `1`, Rotation `(12, -20, 0)` градусов, Zoom `min=0.65, max=2.4, initial=1`.
Scale и zoom должны быть положительными конечными числами; `min <= initial <= max`.
Rotation задаётся конечными числовыми литералами в градусах. При именованных аргументах
нужно передавать все имена соответствующего вызова; смешивание positional/named запрещено.

Неизвестные объявления, повторные Item/View/settings и неправильные аргументы — ошибка.
Один item ID может иметь одно inspection definition. Повторная привязка возвращает
`ITEM_INSPECTION_DUPLICATE_BINDING`. Неизвестный item отклоняется до RMB, с source span
и предложением похожего ID, если оно найдено.

## Управление и обновление

В обычном inventory/container наведите курсор на inspectable stack и нажмите ПКМ.
ЛКМ с перетаскиванием вращает предмет, колесо меняет масштаб в заданных пределах,
Esc возвращает к исходному inventory/container, если menu ещё действителен.
Предметы без inspection definition и пустые слоты сохраняют vanilla RMB.
Фон осмотра — тёмно-фиолетовый градиент из шейдера Echo Blackspace.
Он включается только на экране осмотра, не требует Oculus и не меняет шейдерпак мира.

Внешние `.inspection.es` перечитываются во время игры. Валидное изменение применяется
для следующего открытия; текущая сессия сохраняет прежнюю revision и настройки.
Ошибка сохраняет последний рабочий каталог. Удаление определения перестаёт перехватывать
RMB после успешного reload и не меняет уже открытый snapshot.
Внешние файлы проверяются раз в 20 client ticks; F3+T перечитывает packaged/resources.
При совпадении canonical source ID приоритет: packaged mod → resource pack → внешний файл.

Размер плашки «Осмотреть» настраивается локальными командами без перезапуска:

```text
/echo_item_inspection prompt
/echo_item_inspection prompt scale 1.5
/echo_item_inspection prompt width 360
/echo_item_inspection prompt height 60
/echo_item_inspection prompt reset
```

`scale` меняет всю плашку вместе с текстом (0.5–3; исходно 1).
`width` задаёт минимальную ширину (0–1200; 0 — автоматически по тексту).
`height` задаёт высоту (24–120; исходно 36). Размеры заданы для экрана
1920×1080 и учитывают разрешение окна. Ширина не обрезает надпись;
если места недостаточно, плашка уменьшается, сохраняя отступы от краёв.
Эти параметры не зависят от масштаба интерфейса игры и определения предмета.
Изменения применяются сразу и сохраняются в `config/echo_item_inspection-client.toml`
(`promptScale`, `promptMinWidth`, `promptHeight`). `reset` возвращает исходные значения.

Dev commands:

```text
/echo_item_inspection list
/echo_item_inspection status
/echo_item_inspection inspect <item-id>
/echo_item_inspection reload
```

`inspect <item-id>` открывает presentation копию default stack для отладки.
Осмотр через ПКМ всегда получает точный stack из slot, со всем его NBT и count.
Offline Content Doctor принимает реальный registry snapshot через `--item-catalog`;
его автоматически создаёт клиент в `config/echo_item_inspection/item-catalog.txt`.

Из репозитория `D:/JDHSeason/mods/echo-content-doctor`:

```powershell
.\gradlew.bat contentDoctor -PcontentRoot="D:\content-pack" -PitemCatalog="D:\minecraft\config\echo_item_inspection\item-catalog.txt"
```

Аргументы CLI: `--resources <content-root> --item-catalog <file> <report.json>`.
Без registry snapshot Doctor сообщает `ITEM_INSPECTION_ITEM_CATALOG_REQUIRED`:
offline проверка не может подтвердить наличие всех vanilla и modded items.
Источник предмета, ещё не зарегистрированный в целевой сборке, не считается существующим.
Используйте экспорт сборки, а не вручную составленный каталог.

Для локальной dev-проверки testmod поставляет `inspection_token` и определения для
stone, written_book, diamond_sword. Testmod не входит в production JAR.

```text
/give @s echo_item_inspection_testmod:inspection_token{CustomModelData:19,Enchantments:[{id:"minecraft:unbreaking",lvl:1s}]} 13
/give @s minecraft:stone 64
/give @s minecraft:cobblestone 64
```

Первый предмет проверяет current custom model и glint; stone — BlockItem;
cobblestone — неизменённый vanilla split-stack. Для обычной generated model выдайте
тот же token без CustomModelData.
