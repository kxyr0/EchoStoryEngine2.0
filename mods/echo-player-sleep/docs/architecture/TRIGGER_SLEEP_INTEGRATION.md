# Trigger Sleep Integration

`echo-trigger-player-sleep-adapter` слушает `PlayerSleepLifecycleEvent.sleepEvent()` и строит Trigger selector:

```text
provider = echo_player_sleep:events
event = CONFIRMED
scenario = jdh:chapter_1/first_night
```

Adapter кэширует `TriggerStorySubscription` по exact key и обновляет индекс при изменении Trigger catalog revision.
На событии не выполняется полный scan Trigger resources.

Trigger не зависит от Sleep contracts: в Trigger selector хранятся строковые provider/event/scenario. Типизация происходит
на границе DSL и adapter.
