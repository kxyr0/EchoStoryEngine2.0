# Sleep Trigger Selectors

Поддержанные typed selectors:

- `PlayerSleep.Confirmed(scenarioId)`
- `PlayerSleep.EnteredDream(scenarioId)`
- `PlayerSleep.Returned(scenarioId)`
- `PlayerSleep.Completed(scenarioId)`
- `PlayerSleep.Failed(scenarioId)`

У каждого selector можно добавить ограничение по номеру подтверждённого vanilla bed sleep игрока:

```echoscript
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCount: 5));
Subscribe(PlayerSleep.Confirmed("jdh:chapter_1/night", sleepCountAtLeast: 5));
```

- `sleepCount: 5` - только ровно пятый сон игрока.
- `sleepCountAtLeast: 5` - пятый и все последующие сны игрока.
- Без параметра selector срабатывает на любой номер сна.
- `sleepCount` и `sleepCountAtLeast` нельзя указывать вместе.

Если одному событию подходят несколько подписок, adapter выбирает более конкретную подписку:
`sleepCount` сильнее `sleepCountAtLeast`, `sleepCountAtLeast` сильнее selector без ограничения, а больший
`sleepCountAtLeast` сильнее меньшего. Конфликт остаётся только между одинаково сильными подписками.

Selector содержит:

- provider: `echo_player_sleep:events`
- event kind: `CONFIRMED`, `ENTERED_DREAM`, `RETURNED`, `COMPLETED`, `FAILED`
- scenario id
- optional sleep count constraint
- fingerprint

Lookup выполняется по provider/event/scenario, затем adapter фильтрует событие по `sleepCount`, если selector задал
`sleepCount` или `sleepCountAtLeast`. Событие другого scenario не рассматривает подписку текущего scenario и не требует
пользовательского `if`.
