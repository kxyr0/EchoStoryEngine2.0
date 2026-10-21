# NPC Interaction Proxy

Minecraft block-use API часто требует `Player`. Forge boundary создаёт
server-only player-compatible proxy только на время use pipeline.

Proxy:

- не является телом NPC;
- не отображается как отдельный сюжетный actor;
- ставится в позицию NPC;
- получает yaw/pitch NPC;
- использует выбранную руку;
- очищается после операции.

Generic proxy provider разрешён только для world-state effects. Menu opening,
proxy riding, inventory/capability/progression mutation возвращают typed failure.
# NI2 proxy use

Proxy выполняет normal server-side block use, но не является доказательством успеха сам по себе.

После use runtime сравнивает journal. Если proxy открыл menu, interaction отклоняется как `NPC_INTERACTION_MENU_UNSUPPORTED`. Если proxy только получил item, interaction отклоняется как `NPC_INTERACTION_PROXY_ITEM_ONLY`. Если proxy начал riding, запускается generic ride handoff до cleanup proxy.
