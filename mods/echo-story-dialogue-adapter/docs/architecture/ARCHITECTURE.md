# Архитектура

Adapter состоит из одного Forge мода и одной Story extension session на активный `MinecraftServer`.

Поток:

1. Forge создаёт `EchoStoryDialogueAdapterMod`.
2. Мод слушает `EchoStoryExtensionRegistrationEvent`.
3. Story Engine создаёт `DialogueBridgeSession` через `DialogueBridgeExtensionFactory`.
4. Session отдаёт один `StoryActionHandler` - `DialoguePlayActionHandler`.
5. Handler через `EchoDialogueServiceLookupEvent` получает server-scoped `DialogueService`.
6. `DialogueService` стартует, запрашивает, отменяет или наблюдает Echo Dialogue session.
7. Terminal public view маппится в `ActionCompletion`.

Adapter не хранит persistence, не компилирует Story или Dialogue resources и не трогает клиентский overlay.
