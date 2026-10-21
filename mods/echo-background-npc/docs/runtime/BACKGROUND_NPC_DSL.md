# Echo Background NPC: авторский DSL

Минимальный рабочий ресурс описывает намерение, а не тики, path nodes или внутренние task ID:

```es
class StudentMira : BackgroundNpcResource
{
    Home("jdh:dorm_mira");

    At("08:40")
    {
        GoTo(
            "jdh:university_gate"
        );
    }

    At("09:50")
    {
        TalkWith(
            StudentAnton,
            "jdh:before_class"
        );
    }

    At("10:00")
    {
        GoTo(
            "jdh:room_204"
        );
    }
}
```

Namespace необязателен. Stable resource ID выводится из namespace data pack и пути файла, поэтому переименование класса его не меняет. `At("HH:mm")` является ежедневным indexed occurrence Echo Time; точное равенство времени и polling по NPC не используются. При поздней загрузке и `Time.Set` восстанавливается последняя stateful-фаза, а `TalkWith` как one-shot не переигрывается.

## Дополнительный авторинг

```es
class UniversityStudent : BackgroundNpcProfileResource
{
    AppearancePool("jdh:university_students");
    WalkSpeed(1.0);
    LocomotionProfile("jdh:standard_humanoid");
}

class StudentMira : BackgroundNpcResource
{
    Profile("jdh:university_student");
    Home("jdh:dorm_mira");
    Appearance("jdh:student_girl_03");
    WalkSpeed(1.05);
    At("09:00") { StayAt("jdh:hallway_window"); }
    At("09:15") { Perform("jdh:read_book", at: "jdh:hallway_window"); }
    At("09:50") { TalkWith(StudentAnton, "jdh:before_class", at: "jdh:hallway_window"); }
}
```

`Appearance` и `AppearancePool` взаимоисключающие. Выбор из pool детерминирован `BackgroundNpcId`. Профиль необязателен; скорость, материализация, LOD, avoidance и частоты имеют runtime defaults.

Appearance pool:

```es
class UniversityStudents : BackgroundAppearancePoolResource
{
    Skin("jdh:student_01");
    Skin("jdh:student_02");
    Skin("jdh:student_03");
}
```

Ambient dialogue линейный и неблокирующий:

```es
class BeforeClass : AmbientDialogueResource
{
    Line("mira", "Ты домашку сделала?", voice: Voice("mira_before_class_01"));
    Line("anton", "Я вообще забыл, что она была.");
    Line("mira", "Прекрасно.");

    OnComplete()
    {
        StartQuest("jdh:find_notebook", range: 8.0);
    }
}
```

Voice-файл необязателен. Он ищется штатным Echo Voice по dialogue ID и stem; отсутствие файла не останавливает текстовую реплику.

## Как связаны NPC, разговор и квест

Диалог хранится в `data/<namespace>/echo_background_npc/ambient_dialogues/**/*.es`. Его ID выводится из пути:
файл `ambient_dialogues/before_class.es` в namespace `jdh` имеет ID `jdh:before_class`.

Разговор вызывает задача одного из NPC. Второй NPC указывается типизированным именем класса, а диалог — его ID:

```es
class StudentMira : BackgroundNpcResource
{
    Home("jdh:dorm_mira");

    At("09:50")
    {
        TalkWith(StudentAnton, "jdh:before_class", at: "jdh:hallway_window");
    }
}
```

В `09:50` Echo Time публикует occurrence, оба NPC должны быть живы, материализованы, находиться в одной dimension и
стоять достаточно близко друг к другу. После этого реплики идут автоматически; полноценный блокирующий Dialogue UI
не открывается.

`StartQuest` принимает ID `QuestResource` из отдельного мода `echo_quests`. Прогресс хранится по паре UUID игрока и
Quest ID, поэтому повторное ежедневное окончание разговора не создаёт новую копию того же квеста. `range` необязателен,
по умолчанию равен 12 блокам и не может превышать 14.

Квест запускается только при естественном завершении последней реплики и только для игрока, которому в этот момент
показывался именно этот ambient-разговор. Сервер повторно проверяет ту же dimension, line of sight и расстояние до
ближайшего из двух NPC. Если игрок ушёл, телепортировался, разговор был прерван расписанием/reload или `echo_quests`
недоступен, квест не приходит позже «из воздуха».

Квест хранится отдельно от Story Engine в
`data/<namespace>/echo_quests/quests/**/*.quest.es`:

```es
class FindNotebookQuest : QuestResource
{
    Title("Потерянный блокнот");
    Initial("find_notebook");

    State("find_notebook")
    {
        Objective("Найдите блокнот Миры");
        On("notebook_found", "return_notebook");
    }

    State("return_notebook")
    {
        Objective("Придите к Мире с блокнотом до 18:00");
        At(
            "18:00",
            when: All(
                Player.InArea("minecraft:overworld", x: 42, y: 64, z: 100, radius: 8),
                Player.HasItem("minecraft:writable_book", count: 1)
            ),
            then: "completed",
            otherwise: "failed"
        );
    }

    State("completed")
    {
        Complete();
    }

    State("failed")
    {
        Fail();
    }
}
```

ID QuestResource должен совпадать со значением в `StartQuest`. `echo_quests` владеет стадиями, переходами и
player-scoped сохранением, а ambient-система отвечает только за безопасный старт возле NPC. В примере Echo Time в
18:00 запускает проверку: игрок должен быть онлайн, находиться в указанной зоне и иметь предмет; иначе квест
переходит в `failed`.
