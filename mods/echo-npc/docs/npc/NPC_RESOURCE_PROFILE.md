# NpcResource: EchoScript 2

```text
class ChrisNpc : NpcResource {
    Id("jdh:chris");
    Display("Крис");
    Skin("jdh:chris");
    NameColor("#8e87f8ff");
}
```

`namespace` необязателен. `Id` можно опустить: namespace ресурса и имя файла
`data/jdh/echo_npc/npcs/.../chris.npc.es` дают `jdh:chris`.
`Display` обязателен. `Skin`, `NameColor`, `DefaultForm` необязательны и принимают
одну строку. Например: `DefaultForm("jdh:sasha/child");`.
Цвет принимает RGB или непрозрачный RGBA. Пути текстур и семантический fingerprint
совпадают со старым профилем, поэтому перевод не меняет identity сохранённых actors.

Старый `Build() { return Npc(...); }` остаётся поддержан.
Смешивать его с ES2 metadata нельзя. Повторы, неизвестные вызовы, блоки, поля,
нестроковые значения и неверные ID возвращают ошибки без установки definition.
