# Cooldown

`Cooldown(Ticks(16))` хранится как ready server tick. Cooldown начинается после принятого cast, синхронизируется клиенту и переживает restart.

Повторный intent до ready tick возвращает `ON_COOLDOWN`, не создаёт projectile и не запускает swing.

