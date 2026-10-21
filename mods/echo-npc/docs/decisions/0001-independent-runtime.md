# 0001 Independent Runtime

## Status

Accepted for N1.

## Context

Echo NPC должен быть новым independent mod. Sibling repositories можно читать как
контекст, но production code N1 не должен зависеть от Story, Dialogue, adapter,
animation или navigation projects.

## Decision

Разделить runtime на standalone modules и разрешить только public EchoScript
artifacts для `.npc.es` parsing. Forge integration живет в отдельном module and
adapter boundary.

## Consequences

- NPC foundation можно тестировать без Minecraft в `npc-core`.
- Forge code не протекает в public contracts.
- Story/Dialogue integration должна появляться отдельным future adapter после N1.
