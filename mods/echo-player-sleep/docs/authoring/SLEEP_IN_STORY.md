# Sleep в Story

Story должен создавать external action payload с player UUID, scenario id и durable command id.

Ключевые action types script adapter:

- `echo_player_sleep:arm`;
- `echo_player_sleep:close_eyes`;
- `echo_player_sleep:wait_until_closed`;
- `echo_player_sleep:enter_dream`;
- `echo_player_sleep:open_eyes`;
- `echo_player_sleep:return_to_bed`;
- `echo_player_sleep:complete_night`;
- `echo_player_sleep:complete`.

Сценарий не выбирается автоматически из глобального списка. Это сохраняет авторский контроль главы, состояния и нужного сна.
