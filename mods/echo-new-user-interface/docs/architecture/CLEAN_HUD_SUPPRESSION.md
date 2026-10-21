# Clean HUD Suppression

Когда preview книги включён, `CleanHudSuppressionPolicy` слушает `RenderGuiOverlayEvent.Pre`.

Policy:

- если preview выключен, event не меняется;
- если текущий overlay равен `echo_new_user_interface:journal_book`, event не отменяется;
- все остальные GUI overlays отменяются через `event.setCanceled(true)`.

Это скрывает игровой HUD во время preview и не создаёт server state, network packets или зависимостей на другие Echo-моды.
