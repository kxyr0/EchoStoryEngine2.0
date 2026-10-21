# Progress

## 0.2.0 live EchoScript hosts

- Script adapter registers Story, Dialogue and Trigger runtime bridges.
- Story and Dialogue remain paused until the durable backpack command reaches a successful terminal outcome.
- Trigger callbacks dispatch idempotent durable commands through the same facade.
- Real host compiler tests cover canonical `Npc.EquipBackpack` and `Npc.UnequipBackpack` authoring.

Статус на 2026-08-04: реализация `0.1.0` завершена, production JAR собран.

- Завершён bytecode-аудит фактических Sophisticated Backpacks/Core JAR.
- Добавлены универсальные body attachments в Echo NPC `0.5.0-SNAPSHOT`.
- Добавлены authoritative markers и durable receipts в Echo Animation
  `0.3.0-SNAPSHOT`.
- Реализованы contracts/core/Forge/script/animation/testmod модули адаптера.
- Реализованы exact payload, persistence, recovery, reservations, ownership,
  navigation/alignment, marker idempotency, renderer, sync, diagnostics и команды.
- Unit/architecture/whitespace/source-shape/JAR checks проходят.
- Forge GameTest с реальным Sophisticated Backpacks проходит.

Не отмечены как пройденные: ручная визуальная приёмка в Minecraft и ручная
crash-kill/restart матрица из `docs/TESTING.md`.
