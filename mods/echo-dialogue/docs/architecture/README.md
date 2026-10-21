# Архитектура

Текущий architecture baseline: D6 dynamic speaker presentation provider boundary поверх D5 speaker visuals, D4 choices/outcomes и D3 public API bridge.

- [ARCHITECTURE.md](ARCHITECTURE.md): модульная структура, runtime flow и ownership.
- [DEPENDENCY_RULES.md](DEPENDENCY_RULES.md): разрешённые и запрещённые зависимости.
- [PROJECT_MAP.md](PROJECT_MAP.md): карта production modules, package counts и final artifact.
- [PORTABILITY.md](PORTABILITY.md): portable persistence boundary и Forge-owned concerns.

Echo Dialogue остаётся standalone Forge 1.20.1 модом. D5 добавляет neutral speaker visual IDs и client-only resource
resolution внутри Echo Dialogue; Echo Dialogue не получает зависимость от Story Engine и не содержит adapter
implementation.

D6 добавляет `dialogue-presentation-api` и public Forge registration event без зависимости от Echo NPC. Provider output runtime-only: он не сохраняется, не входит в fingerprint и не меняет network protocol.
