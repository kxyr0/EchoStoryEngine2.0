# Sleep Event Publication

Sleep event создается только после сохранения `PlayerSleepSessionSnapshot`.

Public payload:

- durable `PlayerSleepEventId`
- `PlayerSleepSessionRef`
- `PlayerSleepScenarioId`
- `PlayerRef`
- origin dimension
- bed position, block and provider
- session revision
- server tick
- selector fingerprint

`PlayerSleepEventPublisher` публикует immutable `PlayerSleepEventSnapshot` через `PlayerSleepLifecycleEvent.sleepEvent()`.
Повторная публикация одного event id в рамках runtime подавляется.
