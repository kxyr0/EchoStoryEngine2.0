# Архитектура Echo Dialogue D6

Echo Dialogue остаётся самостоятельным Forge 1.20.1 модом для server-authoritative dialogue sessions. D6 поддерживает линейные строки, ordered branching choices, explicit terminal outcomes, optional authored speaker visuals и runtime-only dynamic speaker presentation provider boundary. Client overlay использует `echo-ui-animations` и D5 visual renderer, но не вводит Echo Story Engine integration, Echo NPC dependency, entity lookup, live skin lookup, voice, quests или generic UI.

## Модули

```text
dialogue-contracts
  <- dialogue-presentation-api
      <- dialogue-forge-api-1.20.1
          <- dialogue-forge-1.20.1

dialogue-contracts
  <- dialogue-core
      <- dialogue-echoscript-adapter
          <- dialogue-forge-1.20.1

dialogue-contracts
  <- dialogue-core
      <- dialogue-persistence
          <- dialogue-forge-1.20.1

```

- `dialogue-contracts`: идентификаторы, structured diagnostics, neutral public service/read-model contracts и D5 `SpeakerVisualRef`.
- `dialogue-presentation-api`: plain Java D6 provider API version `1.2`, request/result/descriptor/session contracts.
- `dialogue-core`: immutable compiled model, deterministic session transitions, authored presentation data, fingerprint и persistence port/checkpoint contract.
- `dialogue-echoscript-adapter`: EchoScript syntax-only binding в compiled dialogue.
- `dialogue-persistence`: schema V1/V2, JSON codec, file repository, backup/temp policy, recovery planning и terminal cleanup.
- `dialogue-forge-api-1.20.1`: public Forge service lookup event и D6 speaker presentation registration event.
- `dialogue-forge-1.20.1`: Forge lifecycle, world-root binding, bounded I/O scheduler, save-before-presentation orchestration, provider catalog/resolution, networking, commands, animated client overlay, client visual resolution/rendering и packaging.

## Runtime Flow

1. Forge reload читает `.dialogue.es` как UTF-8 и компилирует valid catalogue.
2. `/echo_dialogue play <id>` создаёт immutable ACTIVE state через `dialogue-core`.
3. Forge host строит `DialogueCheckpoint` и ставит save в bounded persistence scheduler.
4. Только после successful repository write host installs state и строит runtime presentation.
5. Перед `DialogueNetwork.sendOpen(...)` Forge host запрашивает active provider catalog по `SpeakerId`; ровно один dynamic result может заменить authored display/visual.
6. При no provider, unavailable, failure, malformed result или ambiguity host отправляет authored fallback.
7. Client overlay sample-ит локальные animation presets и optional visual resources; это не меняет server revision и не становится authority.
8. Client отправляет только bounded input: `ADVANCE` для line или `SELECT_CHOICE` для текущего visible choice.
9. Player logout снимает attachment, но durable ACTIVE snapshot остаётся.
10. Новый host загружает retained snapshots из world-scoped storage и заново создаёт provider sessions.
11. Recovery planner разрешает exact compiled fingerprint match, terminal retention или blocked recovery.
12. Player login reattaches same current presentation без изменения domain revision.

## Ownership

`dialogue-core` не знает `Path`, `File`, JSON, Gson, Forge, Minecraft, NBT, texture objects или filesystem moves. Persistence DTO не является network/UI DTO. Client overlay/cache state не является authority и не сохраняется.

Forge владеет привязкой к world directory, provider registration lifecycle и thread ownership: blocking persistence I/O выполняется на host-owned serial worker, а provider resolution, mutation и network delivery выполняются на server thread через completion drain.
