# Writing quests in EchoScript 2

Place quest resources under `data/<namespace>/echo_quests/quests/`:

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
                Player.InArea(
                    "minecraft:overworld",
                    x: 42,
                    y: 64,
                    z: 100,
                    radius: 8
                ),
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

The resource ID is inferred from its data-pack path. For example,
`data/jdh/echo_quests/quests/chapter_1/find_notebook.quest.es` becomes `jdh:chapter_1/find_notebook`. Use one optional
`Id("namespace:path")` call only when a file must move without changing an existing save identity.

`Initial` must point to an active state. Every `On(event, target)` transition is deterministic. `Complete()` and
`Fail()` create terminal states and cannot contain outgoing transitions. Starting an existing quest is idempotent;
terminal quests do not silently restart.

`At` is evaluated when Echo Time crosses its strict 24-hour `HH:mm` value while the quest is in that state. Its
`when`, `then`, and `otherwise` named arguments are required, both target states must exist, and one state cannot
declare the same time twice. Daily time entries are indexed in Echo Time instead of being polled per quest.

Available conditions:

- `Player.InArea(dimension, x: ..., y: ..., z: ..., radius: ...)` checks the online player's dimension and 3D
  distance from the point;
- `Player.HasItem(item, count: ...)` counts matching stacks in main inventory, armor, and offhand without consuming
  them; `count` defaults to `1`;
- `All(...)`, `Any(...)`, and `Not(...)` compose conditions.

All player checks are server-authoritative. If the player is offline at the occurrence, the complete `when`
expression is treated as false, including a top-level `Not`, so the quest moves to `otherwise`.

The system that observes gameplay dispatches the event. The system offering a quest is responsible for eligibility
such as distance, dimension, line of sight, or dialogue completion before calling `QuestService.start`.
