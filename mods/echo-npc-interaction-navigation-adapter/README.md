# Echo NPC Interaction Navigation Adapter

Версия: `0.1.0`.

Adapter связывает public API `echo-npc-interaction` с public API
`echo-navigation`. Он выбирает достижимый approach, создаёт `NavigationRequest`,
ожидает arrival, отменяет принадлежащий `NavigationMoveId` и не импортирует
navigation implementation.

Adapter не содержит pathfinding и не создаёт второй route motor.

