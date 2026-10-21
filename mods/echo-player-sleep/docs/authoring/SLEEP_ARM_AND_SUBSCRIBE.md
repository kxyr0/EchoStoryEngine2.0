# Sleep Arm And Subscribe

`Sleep.Arm(scenarioId)` и `Subscribe(PlayerSleep.Confirmed(scenarioId))` решают разные задачи.

`Sleep.Arm` - runtime-состояние игрока. Следующий успешный vanilla bed sleep этого игрока может стать сюжетным сном.
Команда не укладывает игрока, не закрывает глаза, не телепортирует и не запускает Story.

`Subscribe` - маршрутизация контента. Она говорит Trigger, какой Story запускать после подтвержденного события сна.

Схема:

```text
Sleep.Arm
-> vanilla confirmation
-> PlayerSleepSession
-> Confirmed selector
-> Trigger subscription
-> Story claim
-> StoryResource
```

Без `Sleep.Arm` обычный сон остается обычным: narrative session, Trigger execution и Story execution не создаются.
При этом подтверждённый vanilla bed sleep всё равно увеличивает per-player `sleepCount`, чтобы следующий сюжетный сон мог
сработать по `sleepCount` или `sleepCountAtLeast`.
В Forge runtime этот счётчик сохраняется в world saved data `echo_player_sleep_counts`.
