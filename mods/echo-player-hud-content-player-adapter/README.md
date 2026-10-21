# Echo Player HUD Content Player Adapter

Адаптер, который отдаёт Искру из Echo Content Player как стандартную метрику Echo Player HUD:
`echo_player_hud:spark`.

Адаптер не сохраняет и не меняет Искру. Он читает public API Echo Content Player на сервере и синхронизирует
на клиент только компактный HUD metric snapshot.
