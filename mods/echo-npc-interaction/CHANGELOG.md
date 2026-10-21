# Changelog

## 0.2.0

- Добавлен NI2-поток generic consequence transfer для `Npc.Interact` без Java-адаптеров под мебельные моды.
- Provider `echo_npc_interaction:world_use` выполняет normal server-side block use через временный proxy, journal фиксирует bounded world/proxy effects.
- Добавлены typed consequences, receipt v2 evidence, menu/item-only rejection и диагностика proxy riding.
- Добавлен generic ride handoff: direct attach while occupied и fallback через `EchoNpcSeatAnchorEntity`.
- Добавлены seat occupancy registry, `/echo_npc_interaction stand_up`, `ride inspect`, `seats`, `seat inspect`, `proxy status`.
- DSL `Npc.Interact` не изменён; добавлен generic `Npc.StandUp`.

## 0.1.0

- Создан neutral contract слой для interaction commands, plans, approaches, receipts,
  statuses, outcomes и diagnostics.
- Добавлен plain Java core для validation, provider selection, active interaction
  ownership, state machine, approach ranking, deterministic IDs и receipt ledger.
- Добавлен Forge API для service lookup, query service, provider registry,
  provider registration event, lifecycle event и execution port.
- Добавлен Forge runtime с built-in provider
  `echo_npc_interaction:vanilla_block_use`.
- Добавлен server-side proxy boundary для Minecraft block-use pipeline и proxy audit.
- Добавлен EchoScript adapter для Story, Dialogue и Trigger hosts.
- Добавлены production commands `/echo_npc_interaction ...`.
- Добавлена русская DSL и архитектурная документация.
