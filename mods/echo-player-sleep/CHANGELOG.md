# Changelog

## 0.2.0

- Added event ledger lookup and `/echo_sleep arm/session/event/claim/diagnostics` commands.
- Added the remaining PS1.1 diagnostic enum values for claim ownership, recovery, stale/duplicate events and
  context inheritance failures.

- Добавлен public API `PlayerSleepEventSnapshot` для typed sleep lifecycle events.
- Добавлены `WAITING_FOR_STORY_CLAIM` и `STORY_CLAIMED`; auto-close после vanilla confirmation удален.
- Добавлен `PlayerSleepStoryClaimService` и one-session-one-claim contract.
- `Sleep.*` Story actions теперь используют inherited sleep context через `PlayerSleepStoryContextBridge`.
- Arm потребляется только после успешного Story claim; при missing/conflict subscription arm сохраняется.
- Добавлена документация по `Sleep.Arm`, `Subscribe`, selectors, context inheritance и failure modes.

## 0.1.0

- Создан независимый multi-module репозиторий Echo Player Sleep.
- Добавлены neutral contracts, core runtime, Forge API, Forge host, EchoScript/Story adapter и testmod.
- Добавлены проверки архитектуры, source shape и русская документация PS1.
