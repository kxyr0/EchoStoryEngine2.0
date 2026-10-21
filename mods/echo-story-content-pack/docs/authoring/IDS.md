# ID conventions

Resource namespace для проекта: `jdh`.

`NpcDefinitionId` описывает reusable NPC definition.

Пример: `jdh:john_father`.

`ActorId` описывает конкретного logical actor в мире.

Пример: `jdh:john_father`.

`SpeakerId` описывает logical dialogue speaker.

Пример: `jdh:john_father`.

Для простого уникального персонажа рекомендуется держать одинаковую строку для `SpeakerId` и `ActorId`, но это разные domain types.

N4 требует именно exact equality canonical strings. Если `SpeakerId` отличается
от `ActorId`, dynamic Dialogue-NPC presentation не сработает и будет показан
authored fallback.

`DialogueId` описывает один dialogue definition.

Пример: `jdh:chapter_1_home_arrival`.

`StoryId` описывает один Story definition.

Пример: `jdh:chapter_1_home_arrival`.

`DialogueOutcomeId` описывает stable terminal result.

Примеры:

- `jdh:told_truth`
- `jdh:lied`

Local dialogue alias короткий и живет только внутри одного `.dialogue.es`.

Пример: `father`.

File names не являются identity authority. Не выводите IDs из file path, display name, class name, entity UUID, line text или choice text.

Для reusable NPC definition:

- `NpcDefinitionId`: `jdh:guard`
- `ActorId`: `jdh:guard_01`, `jdh:guard_02`, `jdh:guard_03`
