# Writing StoryResource in EchoScript 2

`StoryResource` описывает последовательность сцен и действий. Это не квест и он не хранит quest state. Квесты
описываются отдельным `QuestResource` из мода `echo-quests`.

Фактический loader path:

`data/<namespace>/echo_story/stories/**/*.es`

Для этого pack: `src/main/resources/data/jdh/echo_story/stories/`.

Новый стандартный профиль описывает намерение, а внутренний граф строит Story Engine:

```es
class ChapterOpeningStory : StoryResource
{
    OnStart()
    {
        Dialogue.Load("jdh:chapter_opening");
        Story.Set("chapter_1.opening_complete", true);
    }
}
```

Файл `chapter_opening.es` в data namespace `jdh` получает Story ID `jdh:chapter_opening`. EchoScript `namespace`, имя
класса и промежуточные папки служат только для организации и не меняют ID. Если существующий stable ID отличается
от имени файла, используется явный override:

```es
Id("jdh:chapter_1_home_arrival");
```

`OnStart()` исполняется сверху вниз. Компилятор сам добавляет start/finish nodes, детерминированные внутренние ID и
`next` transitions. Автору не нужны `Build()`, `StoryResourceDef`, `Nodes()`, `Start(...)`, `Finish(...)` и ручные
связи `next:`.

Основные ES2 intrinsics:

- `Story.Set("variable", value)`;
- `Story.Require("variable", expectedValue)`;
- `Dialogue.Load("namespace:id", output: "outcome")`;
- `Story.Load("namespace:id")`;
- domain sugar: `Npc.*`, `Player.*`, `Time.*`, `View.*`, `Environment.*`, `Screen.*`;
- `Action(type: "namespace:action", output: "adapter_output_key", ...)` для adapter action без отдельного sugar.

`Action(...)` принимает плоские named scalar arguments. Reserved names: `type`, `output`. Значение `output`
совпадает с ключом результата adapter-а и становится Story variable с тем же именем.

`Story.Require(...)` при несовпадении завершает Story как FAILED и не является `if/else`. Полноценный authored
branch graph появится только вместе с отдельным high-level branch intrinsic.

Legacy `Build()` + `Nodes()`/method `OnStart()` остаётся readable для совместимости, но смешивать legacy и ES2
declarations в одном ресурсе нельзя. Новые файлы и шаблоны используют только ES2.

Quest authoring: `../../../echo-quests/docs/authoring/WRITING_QUESTS.md`.
