# Diagnostics

Основные коды имеют prefix `ENPC-SB-`:

- `SOURCE-MISSING`, `SOURCE-CHANGED`, `NOT-BACKPACK`;
- `NPC-NOT-MATERIALIZED`, `WORLD-MISMATCH`, `APPROACH-FAILED`;
- `RESERVED`, `TRANSITION-BUSY`, `DESTINATION-OCCUPIED`;
- `PERSISTENCE-FAILED`, `REMOVE-FAILED`, `PLACE-*`;
- `MARKER-OUT-OF-ORDER`, `MARKER-DUPLICATE`;
- `RECOVERY-PAYLOAD-MISSING`, `RECOVERY-DUPLICATE-CONFLICT`,
  `RECOVERY-DESTINATION-OCCUPIED`.

Structured logs используют fields `event`, `actor`, `animation`, `playback`,
`marker`, `outcome`, `code`. Команды `inspect`, `transition`, `payload`,
`reservations`, `diagnostics` и `recover` позволяют диагностировать состояние
без чтения raw journal. Команды не печатают содержимое всех slots по умолчанию,
чтобы не раздувать log и не раскрывать лишние данные.
