# Animation markers

Основной adapter по умолчанию работает в `IMMEDIATE`. Optional
`echo_npc_sophisticated_backpacks_animation_adapter` слушает generic
server-authoritative `AnimationMarkerEvent`; Echo Animation не знает о
рюкзаках.

Semantic IDs:

- `echo_npc_sophisticated_backpacks:take_from_world`;
- `echo_npc_sophisticated_backpacks:attach_left_hand`;
- `echo_npc_sophisticated_backpacks:attach_right_hand`;
- `echo_npc_sophisticated_backpacks:attach_back`;
- `echo_npc_sophisticated_backpacks:detach_from_back`;
- `echo_npc_sophisticated_backpacks:place_in_world`.

Equip order: `take_from_world` → optional hand → `attach_back`.
Unequip order: `detach_from_back` → optional hand → `place_in_world`.
Out-of-order marker возвращает `ENPC-SB-MARKER-OUT-OF-ORDER`; duplicate exact
occurrence игнорируется. Завершение animation без обязательного marker не
считается успешным backpack transition.

Timeline можно менять без Java/Story изменений: если `attach_back` перенесён с
tick 18 на tick 21, payload остаётся в HAND до tick 21. Generic marker definition
хранится в Echo Animation archive и receipt persistence.

Целевой binding resource выбирается по ActorId + active form + transition type и
может указывать authored equip/unequip AnimationId. Невалидный явно заданный
binding должен отклоняться как `NPC_BACKPACK_ANIMATION_BINDING_INVALID`, без
молчаливого fallback в IMMEDIATE.

При reload допускаются отдельные bindings для одного ActorId и разных form ID.
Exact active-form binding имеет приоритет, а `form: "*"` используется только
как явный wildcard fallback.
