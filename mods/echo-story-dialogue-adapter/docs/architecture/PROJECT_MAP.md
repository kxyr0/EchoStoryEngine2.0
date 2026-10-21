# Карта проекта

- `src/main/java/io/github/kxyr0/echostorydialogueadapter/forge`
  - `EchoStoryDialogueAdapterMod` - Forge entrypoint и регистрация Story extension factory.
- `src/main/java/io/github/kxyr0/echostorydialogueadapter/bridge`
  - `DialogueBridgeExtensionFactory` - создаёт server-scoped extension session.
  - `DialogueBridgeSession` - отдаёт descriptor и action handlers, закрывает observations.
  - `DialoguePlayActionHandler` - start/recover/cancel flow для `echo_dialogue:play`.
  - `DialogueActionPayload` - строгий decoder payload.
  - `DialogueBridgeIds` - extension/action ids и session-id mapping.
  - `DialogueServiceLookup` - synchronous Forge lookup `DialogueService`.
  - `DialogueCompletionMapping` - terminal Dialogue view -> Story completion.
- `src/architectureTest/java`
  - architecture boundaries для публичных API и запретов.
- `docs`
  - русская документация D3.
