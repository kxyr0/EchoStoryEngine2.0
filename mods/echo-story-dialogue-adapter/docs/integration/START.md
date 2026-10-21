# Start Flow

`DialoguePlayActionHandler.start` выполняет:

1. Проверяет action type `echo_dialogue:play`.
2. Декодирует `dialogueId` и `audienceId`.
3. Получает `DialogueService` через `EchoDialogueServiceLookupEvent` для текущего `MinecraftServer`.
4. Создаёт `DialogueStartRequest` с caller-provided deterministic `DialogueSessionId`.
5. Передаёт request в `DialogueService.start`.
6. Если view активный, подписывается на terminal transition.
7. Если view уже terminal или blocked, сразу отправляет Story completion.

`SERVICE_UNAVAILABLE` возвращает `ActionInvocationResult.rejected`, чтобы Story не считал action принятым.

`ACCEPTED` от Dialogue не означает terminal acknowledgement. Story completion отправляется только после terminal view.
