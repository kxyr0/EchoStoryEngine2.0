# NPC Interaction Proxy Audit

## Решение

NI1 использует Forge `FakePlayer` только как compatibility context для server-side
Minecraft block-use API. Это не тело NPC и не сюжетная identity.

## Инварианты

- Proxy создаётся server-only.
- Proxy не должен оставаться видимым player entity.
- Proxy ставится в позицию NPC перед use.
- Proxy yaw/pitch синхронизируются с выбранным hit point.
- Proxy inventory очищается после use.
- Если открылось меню, результат отклоняется как
  `NPC_INTERACTION_PLAYER_BOUND_UNSUPPORTED`.
- Если proxy оказался riding/seated, riding очищается и результат отклоняется как
  `NPC_INTERACTION_REQUIRES_SEAT_PROVIDER`.

## Что не считается успехом

- `InteractionResult.SUCCESS` без world-state witness.
- Proxy riding state.
- Client packet simulation.
- Прямое изменение known block state.

## Допустимые generic результаты

- Изменился block state fingerprint.
- Provider подтвердил world-state effect.
- Специализированный provider подтвердил NPC attachment.

