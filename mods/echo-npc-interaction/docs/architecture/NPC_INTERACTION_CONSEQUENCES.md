# NPC Interaction Consequences

NI2 не считает `InteractionResult.SUCCESS` или `CONSUME` доказательством полезного результата.

После normal server-side block use runtime сравнивает bounded journal до и после операции:

- target BlockState;
- target BlockEntity fingerprint;
- entities рядом с target;
- proxy vehicle;
- proxy menu;
- proxy inventory;
- proxy pose/position.

Классификация typed:

- `BLOCK_STATE_CHANGED`;
- `BLOCK_ENTITY_CHANGED`;
- `ENTITY_SPAWNED`;
- `PROXY_STARTED_RIDING`;
- `PROXY_RECEIVED_ITEM`;
- `PROXY_STATE_ONLY`;
- `MENU_OPENED`;
- `NO_OBSERVABLE_EFFECT`;
- `MULTIPLE_EFFECTS`.

World-state последствия завершают interaction без переноса на NPC. `MENU_OPENED`, item-only и proxy-only эффекты отклоняются typed failure.
