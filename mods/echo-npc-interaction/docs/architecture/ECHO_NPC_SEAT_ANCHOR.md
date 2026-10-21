# Echo NPC Seat Anchor

`EchoNpcSeatAnchorEntity` является generic fallback vehicle для результата "proxy начал сидеть".

Anchor:

- невидим;
- не имеет collision и AI;
- хранит target block;
- хранит owning `NpcId`;
- хранит interaction command ID;
- удаляется после dismount;
- удаляется, если target block исчез.

Anchor не копирует foreign Entity NBT и не создаёт EntityType чужого мода. Он переносит только нейтрально наблюдаемый результат: passenger position, yaw, target position и occupancy ownership.
