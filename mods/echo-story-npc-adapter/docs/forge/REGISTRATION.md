# Регистрация Forge

`EchoStoryNpcAdapterMod` subscribes to Forge event bus.

На `EchoStoryExtensionRegistrationEvent` адаптер регистрирует
`StoryNpcExtensionFactory`. Factory возвращает один descriptor и создает
server-scoped экземпляры `StoryNpcExtensionSession`.

На server tick активные sessions получают `NpcService` и обрабатывают
observations. При остановке server или закрытии Story session observations
очищаются.
