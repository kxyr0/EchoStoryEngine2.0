# Новые Echo-модули: индекс документации

Этот раздел описывает только новые игровые модули, созданные для механики наблюдения и формы мысли. Существующие системы NPC, Dialogue, Story, Cutscene и Player Actor здесь не переописываются; упоминаются только их точки интеграции.

## Модули

| Мод | Назначение | Документ |
|---|---|---|
| `echo_concentration` | Режим концентрации, радиальная десатурация и приглушение звука | [Echo Concentration](echo-concentration.md) |
| `echo_observation` | Observation DSL, каталог точек, server-authoritative focus и прогресс | [Observation DSL](observation-dsl.md) |
| `echo_observation_authoring` | Внутриигровая расстановка и picker точек | [Observation Authoring](observation-authoring.md) |
| `echo_thought_space` | Непаузящий 2D-холст завершённых мыслей | [Echo Thought Space](echo-thought-space.md), [Config DSL](thought-space-config-dsl.md) |

## Типичный рабочий поток

1. Автор создаёт `ObservationResource` и declarations `Observation` в `.es`.
2. Нерасставленные declarations остаются валидными, но не участвуют в gameplay.
3. В одиночной игре автор включает `F6`, выбирает declaration через `Tab` и ставит её клавишей `Insert`.
4. Story активирует нужный Observation resource для конкретного игрока.
5. Игрок включает концентрацию клавишей `Z`, наводится на точку и удерживает клавишу инвентаря `E`.
6. Сервер подтверждает completion, сохраняет прогресс и выполняет `OnComplete`.
7. `Text(...)` показывается стандартным Dialogue overlay, а `Thought(...)` становится узлом Thought Space.
8. Игрок открывает Thought Space клавишей `P`, перемещает холст удержанием ЛКМ и масштабирует колесом относительно курсора.
9. Стартовые узлы можно объявить напрямую через ES2 `Thought Name() { Text(...); }`; если для пары объявлен `Combination Name()`, игрок соединяет их drag-жестом и получает третий выводимый узел.

## Важное различие

- `Text(...)` — короткая inspection-реплика самой Observation. Она показывается неинтерактивным стандартным Dialogue overlay и не записывается на полотно.
- `Thought(...)` — внутренняя мысль. Она не показывается поверх игры и после completion записывается только в Thought Space.
- `Dialogue.Load("namespace:id")` — запуск полноценного авторского диалога из отдельного `.dialogue.es`.
- `Story.Start("namespace:id")` — запуск StoryResource.

`Text(...)` не заменяет полноценный Dialogue DSL и не создаёт dialogue-сессию. Если одновременно начинается настоящий диалог, он получает приоритет. `Text(...)` и `Thought(...)` разрешено указывать вместе или по отдельности.
