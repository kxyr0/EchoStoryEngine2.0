# Npc.PlaceAt

`Npc.PlaceAt` ставит уже существующего NPC в точку сцены. В N5 это отдельная
команда, потому что она использует активные form dimensions при проверке места.

Командный аналог:

```text
/echo_npc place_at <npc_id> <pos> <yaw> <pitch>
```

Отличие от старого `teleport`: authored scene placement должен проверять
занятость и габариты текущей формы до durable записи. Если child form проходит,
а adult form нет, сначала выбери позицию под adult body или вызывай
`validate_space`.

Проверка:

```text
/echo_npc form validate_space jdh:sasha jdh:sasha/adult
```

Коды отказа смотри в `docs/diagnostics/NPC_FORM_DIAGNOSTICS.md`.
