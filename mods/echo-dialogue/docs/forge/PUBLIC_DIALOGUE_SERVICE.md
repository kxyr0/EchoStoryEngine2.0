# Public Dialogue Service

D3 добавляет минимальный публичный API для внешнего Story-Dialogue adapter.

Public contracts находятся в `dialogue-contracts`:

- `DialogueService`
- `DialogueStartRequest`
- `DialogueStartResult`
- `DialogueQueryResult`
- `DialogueCancelResult`
- `DialogueOperationStatus`
- `DialogueSessionView`
- `DialogueSessionStatus`
- `ChoiceId`
- `DialogueOutcomeId`
- `DialogueTerminalObserver`
- `DialogueTerminalEvent`
- `DialogueObservationHandle`

Forge lookup находится в `dialogue-forge-api-1.20.1`:

- `EchoDialogueServiceLookupEvent`

Runtime provider находится в `dialogue-forge-1.20.1`:

- `ForgeDialoguePublicService`
- `DialoguePublicSessionReadModel`
- `ForgeDialogueCompositionRoot`

Правила:

- lookup выполняется для конкретного `MinecraftServer`;
- provider отвечает только после завершения recovery host lifecycle;

D6 dynamic speaker presentation provider registration is separate from `DialogueService` lookup. It uses `EchoDialogueSpeakerPresentationRegistrationEvent` and exposes no `DialogueService`, host implementation or persistence state to presentation providers.
- start использует caller-provided `DialogueSessionId`;
- query возвращает active, terminal или blocked public view;
- completed terminal view предоставляет `terminalOutcomeId()`;
- cancel не раскрывает implementation internals;
- terminal observers получают событие после durable terminal save;
- API не раскрывает `DialogueHost`, repository, compiled dialogue, packets, overlay или `ServerPlayer`.
