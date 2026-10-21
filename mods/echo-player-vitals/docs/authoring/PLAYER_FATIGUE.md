# Усталость игрока

`PlayerFatigue` хранит raw-значение `current` и `maximum`.

- `0` означает полностью отдохнувшего игрока.
- `maximum` означает полностью истощённого игрока.
- Инвариант: `0 <= current <= maximum`, `maximum > 0`.

HUD не должен показывать raw fatigue напрямую. Для полоски используется обратная величина:

```text
reserve = maximum - current
normalizedReserve = reserve / maximum
```

Примеры при `maximum = 100`:

- `fatigue = 0` даёт `reserve = 100%`.
- `fatigue = 50` даёт `reserve = 50%`.
- `fatigue = 75` даёт `reserve = 25%`.
- `fatigue = 100` даёт `reserve = 0%`.

В версии `0.1.0` автоматического накопления усталости нет. Усталость меняется только публичными командами и API:

- `/echo_vitals fatigue set <player> <value>`
- `/echo_vitals fatigue add <player> <value>`
- `/echo_vitals fatigue reduce <player> <value>`
- `/echo_vitals fatigue restore <player>`
