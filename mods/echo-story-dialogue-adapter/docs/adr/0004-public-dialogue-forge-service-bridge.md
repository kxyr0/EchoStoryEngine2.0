# ADR 0004: Public Dialogue Forge Service Bridge

Статус: принято.

Решение: Echo Dialogue предоставляет public Forge bridge module `dialogue-forge-api-1.20.1` с
`EchoDialogueServiceLookupEvent`.

Причины:

- adapter должен получить server-scoped `DialogueService` без implementation imports;
- global mutable registry не нужен;
- lookup явно привязан к `MinecraftServer`;
- zero или duplicate providers становятся диагностируемой ошибкой.

Bridge не содержит persistence, networking, client overlay, compiled dialogue implementation или resource loading.
