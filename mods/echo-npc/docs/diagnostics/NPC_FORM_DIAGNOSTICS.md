# NPC Form Diagnostics

Основные коды N5:

- `NPC_FORM_ID_CONFLICT`: два `.form.es` объявили один `NpcFormId`;
- `NPC_FORM_NOT_FOUND`: `defaultForm` или `SetForm` ссылается на отсутствующую форму;
- `NPC_FORM_ALREADY_ACTIVE`: target form уже активна;
- `NPC_FORM_SPACE_BLOCKED`: текущая или target position не подходит под dimensions;
- `NPC_FORM_ACTOR_NOT_FOUND`: actor отсутствует;
- `NPC_FORM_SLEEPING`: actor спит, сначала нужен `Npc.WakeUp`;
- `NPC_FORM_PENDING_MOVE`: actor уже ждет move/materialization mutation;
- `NPC_FORM_SERVICE_STOPPED`: Echo NPC runtime не готов.

Отладочные команды:

```text
/echo_npc forms list
/echo_npc forms inspect <form_id>
/echo_npc form get <npc_id>
/echo_npc form validate_space <npc_id> <form_id>
```

При ошибке сначала проверь `/reload`, затем список форм, затем active actor view.
